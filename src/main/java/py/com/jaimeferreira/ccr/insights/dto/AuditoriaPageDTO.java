package py.com.jaimeferreira.ccr.insights.dto;

import java.util.List;

public class AuditoriaPageDTO {

    private List<AuditoriaDTO> content;
    private long totalElements;

    public AuditoriaPageDTO(List<AuditoriaDTO> content, long totalElements) {
        this.content = content;
        this.totalElements = totalElements;
    }

    public List<AuditoriaDTO> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
}
