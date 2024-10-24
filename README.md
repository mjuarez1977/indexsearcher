# indexsearcher

This is a 48-hour "coding challenge" submitted about a decade ago when interviewing for a job.  Definitely outdated now, but it was fully functional, and met the requirements given.

**Original instructions below:**

---------------------------------------------------

Write a simple search engine which can locate a musical group from the attached dataset and return its id.

Guidelines
1. Yes, we're really only looking for the ID (or IDs) associated with a particular band name.
   a. No need to return (band name, band ID) pairs or the original text line.

2. Imagine the dataset is 1TB in size. Your solution should scale to work on input of this size.  It turns out the sample input file we've given you isn't that big and a number of simple solutions would probably work with input of this size.  Don't despair if this part seems ambiguous. Maybe you haven't worked with large datasets before? Maybe you're just pressed for time? That's OK.
   a. Do a few Google searches.
   b. If the Google results make sense to you, then throw in an optimization or two for good measure.
   c. Or let us know which ones seem most promising/interesting if you just don't have the time for point b.
   Anything that gives us insight into how you approach slightly more open-ended problems and research tasks will be much appreciated.


---------------------------------------------------

**Notes/comments I sent:**

As far as design notes/considerations:
I included two apps, a searcher that loads everything in RAM, and a disk-based searcher.

The`InMemorySearcher` simply loads everything into a HashMap, and is only bound by the amount of RAM available
to the JVM. This will work if the database is small enough to keep in memory, and is pretty simple. Obviously it
wouldn't work with TB+ level input files, but I included it for comparison.

The `DiskIndexSearcher` is the real application. It works based on the assumption that there's probably a small amount
of bands that comprise most of the queries, and also that a lot of searches would return no band by that name. Since
disk access is orders of magnitude slower than RAM, I included two "caching" layers for this: An LRU cache of
arbitrary size, that stores the n most recently queried bands, and a large bloom filter with 3 hashes that will return
immediately if the band name doesn't exist. Only if those two layers fail, does it go down to disk to read the indexes.

The disk index implementation is basically an arbitrary number of "Index Structures" which are really file pairs. The
larger the dataset, the more files the system should be configured to use. Right now it's hard-coded to 100 index
structures.

The idea was to create an "index file" .idx with maybe a serialized structure/index that contains all band names, along
with pointers to the main data file, so that access is directly through seek offsets. However, I started down this path,
and realized it would take much more time than I had, so I settled on a naive way to store them: The band/id pairs are
simply appended to the main .dat file, and the .idx files are not used.

So, when initializing the indexes, all the data is read from the input file. Each band name is "hashed" using a CRC32, 
and assigned one of the available index files, and the bloom filter is updated. Because CRC32 has pretty good
distribution, on average, all index files hold roughly the same amount of data. Because the bloom filter is already up
to date, queries for most non-existent band names can be rejected immediately. However, when a band name does
exist, it goes down to the corresponding index file, and reads the file. It looks for the "key" (band name) throughout
the entire file, since a band can have multiple IDs, and they're all guaranteed to be on the same index.

I tested the solution with random input files I created, up to ~1GB total, and using 1000 index structures. The
initialization takes a bit, but once that's done, performance is really good. However, it definitely starts slowing down
beyond that, since the queries have to read a whole file every single time, and that starts getting really slow with larger
files.

I believe this solution (along with improvements detailed below, specifically around byte packing/compression, and
proper indexes per file) would scale to TB+ level. If the server was large enough, with enough RAM and fast local
SSD storage, maybe only one would be needed, but the general case should consider splitting the index across
multiple machines, adding server replicas, and communicating queries through a REST api internally, especially when
talking about high availability. That way, scaling the index is simply a matter of adding machines and some
configuration.


**Considerations/thoughts:**
- I assumed the tsv file included a representative input sample.
- The longest band names were less than 300 characters, median band name was around 25 characters, and all
  band IDs were 10 characters or less.
- All band IDs had the same four character prefix "/m/0".
- Except for the above four characters, the rest of the band IDs was all underscores, numbers or lower case letters,
  except for the fact that there were no vowels.
- Some bands had multiple IDs (less than 4% of the total), but there were no entire duplicate rows.
- Band names included the entire range of Unicode characters, including right-to-left encodings
- All reads and writes to disk are done with the UTF-8 character set, so the band names are appropriately stored and
  retrieved.
- There were at least a few lines in the file that were invalid, e.g., the first character was a tab followed by a band ID,
  so there was no band name. These lines are skipped.
- The application sets up proper shutdown hooks, but they don't really do much. In a heavy concurrency scenario,
  there would have to be much more housekeeping to ensure data is not corrupted, all requests finish correctly, etc.
- The current solution is inefficient in that it creates the disk indexes every single time, e.g. it doesn't load the files if
  they're already there. This is obviously not realistic, since the initial processing could take quite some time.

**Things that I didn't have time to implement, and which would definitely need to be added if this was a production-level app:**
- Real tests
- Loading of configuration from properties. Right now, a lot of parameters are hard-coded (location of index catalog
  on disk, size of LRU cache, size of bloom filter bitset, etc). This would have been nice to have configurable via
  properties.
- A way to sanitize file input. I just assumed the file was correct.
- A real serialized index structure in the segment header files for direct offset access to the band IDs. This would
  make the system much faster.
- A much more packed and compressed way to store data, both band names and IDs. The band IDs, since they
  only really have 6 characters (all the prefixes are the same), and those are only underscores, letters and digits without
  vowels, it's possible to represent them with just 5 bits per character, which means a packed representation of IDs
  would use up only 4 bytes. Since the TB+ scenario would involve billions of these IDs, saving even a few bytes per ID
  is significant.
- Thread safety around concurrent querying and updating of disk files. There could be a queue of requests (both
  reads and updates), fronted by a single thread per index segment. Implementing read/write locks in that scenario will
  avoid corrupt indexes, and/or invalid reads.
- Ability for the app to recognize that indexes are already in place, and only add updates/deletes to them, instead of
  recreating completely every single time.