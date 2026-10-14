package com.irms.controller;

import com.irms.model.RecruitmentRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RecruitmentRequestCopyTest {
    @Test
    public void copyContainsOnlyNewTitleAndRecruitmentContent() {
        RecruitmentRequest source = new RecruitmentRequest();
        source.setId("source-id");
        source.setCode("REQ-001");
        source.setTitle("Tuyển dụng Lập trình viên Java");
        source.setPositionTitle("Lập trình viên Java");
        source.setDepartmentId("source-department");
        source.setHiringManagerId("source-manager");
        source.setStatus("OPEN");
        source.setJobDescription("Mô tả công việc");
        source.setJobRequirements("Yêu cầu ứng viên");

        RecruitmentRequest copy = RecruitmentRequestServlet.createDraftCopy(source);

        assertNull(copy.getId());
        assertNull(copy.getCode());
        assertNull(copy.getDepartmentId());
        assertNull(copy.getHiringManagerId());
        assertTrue("DRAFT".equals(copy.getStatus()));
        assertEquals("Bản sao - Tuyển dụng Lập trình viên Java", copy.getTitle());
        assertEquals(source.getPositionTitle(), copy.getPositionTitle());
        assertEquals(source.getJobDescription(), copy.getJobDescription());
        assertEquals(source.getJobRequirements(), copy.getJobRequirements());
    }

    @Test
    public void copiedTitleDoesNotExceedDatabaseLimit() {
        RecruitmentRequest source = new RecruitmentRequest();
        source.setTitle(repeat('a', 255));

        RecruitmentRequest copy = RecruitmentRequestServlet.createDraftCopy(source);

        assertEquals(255, copy.getTitle().length());
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }
}
