package com.mjuarez.pandora;

/**
 * Class that holds each of the segment/index pairs
 */
public class IndexStructure {
    private final Segment segment;

    private final SegmentHeader segmentHeader;
    public IndexStructure(Segment segment, SegmentHeader segmentHeader) {
        this.segment = segment;
        this.segmentHeader = segmentHeader;
    }

    public Segment getSegment() {
        return segment;
    }

    public SegmentHeader getSegmentHeader() {
        return segmentHeader;
    }

    @Override
    public String toString() {
        return "IndexStructure{" +
                "segment=" + segment +
                ", segmentHeader=" + segmentHeader +
                '}';
    }
}
