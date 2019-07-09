package com.sumologic.jenkins.jenkinssumologicplugin.constants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParallelNodeTypeEnumTest {

    @Test
    public void TestEnums() {
        assertEquals("NORMAL", ParallelNodeTypeEnum.NORMAL.toString());
        assertEquals("PARALLEL_START", ParallelNodeTypeEnum.PARALLEL_START.toString());
        assertEquals("PARALLEL_END", ParallelNodeTypeEnum.PARALLEL_END.toString());
        assertEquals("PARALLEL_BRANCH_START", ParallelNodeTypeEnum.PARALLEL_BRANCH_START.toString());
        assertEquals("PARALLEL_BRANCH_END", ParallelNodeTypeEnum.PARALLEL_BRANCH_END.toString());
    }

}