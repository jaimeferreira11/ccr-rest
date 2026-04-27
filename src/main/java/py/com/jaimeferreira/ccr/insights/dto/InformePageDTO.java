package py.com.jaimeferreira.ccr.insights.dto;

import java.util.List;

public class InformePageDTO {

    private List<InformeDTO> content;
    private long totalElements;

    public InformePageDTO(List<InformeDTO> content, long totalElements) {
        this.content = content;
        this.totalElements = totalElements;
    }

    public List<InformeDTO> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
}
