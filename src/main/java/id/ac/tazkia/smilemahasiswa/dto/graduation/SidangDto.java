package id.ac.tazkia.smilemahasiswa.dto.graduation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SidangDto {
    private String id;
    private String komentar;
    private BigDecimal nilaiA;
    private BigDecimal nilaiB;
    private BigDecimal nilaiC;
    private BigDecimal nilaiD;
    private String beritaAcara;
}
