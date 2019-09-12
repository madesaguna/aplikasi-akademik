package id.ac.tazkia.akademik.aplikasiakademik.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiRfidDto {
    private Integer idAbsen;
    private String nama;
    private String rfid;
    private Boolean sukses = true;
    private String pesanError;
    private Integer jumlah;
}
