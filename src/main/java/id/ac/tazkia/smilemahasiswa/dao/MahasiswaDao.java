package id.ac.tazkia.smilemahasiswa.dao;

import id.ac.tazkia.smilemahasiswa.dto.ListAngkatanDto;
import id.ac.tazkia.smilemahasiswa.dto.machine.ApiRfidDto;
import id.ac.tazkia.smilemahasiswa.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MahasiswaDao extends PagingAndSortingRepository<Mahasiswa,String> {
    Mahasiswa findByUser(User user);

    @Query("select distinct m.angkatan from Mahasiswa m order by m.angkatan asc")
    Iterable<Mahasiswa> cariAngkatan();

    Mahasiswa findByNim(String nim);

    Mahasiswa findByNimAndStatus(String nim,StatusRecord status);

    @Query("select new id.ac.tazkia.smilemahasiswa.dto.machine.ApiRfidDto(m.idAbsen,m.nama,m.rfid,true ,'',0) from  Mahasiswa m where m.status = :status and m.rfid is not null")
    List<ApiRfidDto> rfidMahasiswa(@Param("status")StatusRecord statusRecord);

    @Query("select m.id from Mahasiswa m where m.nim = :nim")
    String cariIdMahasiswa(@Param("nim")String nim);

    @Query("select m from Mahasiswa m where m.status = :status and m.angkatan = :angkatan and m.idProdi = :prodi")
    Iterable<Mahasiswa> carikelas(@Param("status")StatusRecord statusRecord,@Param("angkatan") String angkatan,@Param("prodi") Prodi prodi);

    @Query("select m from Mahasiswa m where m.status = :status and m.angkatan = :angkatan and m.idProdi = :prodi and m.idKonsentrasi = :konsentrasi")
    Iterable<Mahasiswa> carikelasKonsentrai(@Param("status")StatusRecord statusRecord,@Param("angkatan") String angkatan,@Param("prodi") Prodi prodi, @Param("konsentrasi") Konsentrasi konsentrasi);

    List<Mahasiswa> findByStatusAndAngkatan(StatusRecord aktif,String angkatan);

    @Query(value = "select angkatan,nama_jenjang from mahasiswa as a inner join prodi as b on a.id_prodi = b.id inner join jenjang as c on b.id_jenjang = c.id group by concat(angkatan,id_jenjang) order by angkatan desc", nativeQuery = true)
    List<Object> angkatanMahasiswa();

    Page<Mahasiswa> findByStatusNotInAndNamaContainingIgnoreCaseOrNimOrderByNim(List<StatusRecord> statusRecords, String nama, String nim, Pageable page);

    Page<Mahasiswa> findByStatusNotInOrderByNim(List<StatusRecord> statusRecords, Pageable page);

    @Query(value = "SELECT MAX(id_absen) FROM mahasiswa;", nativeQuery = true)
    Integer cariMaxAbsen();

    List<Mahasiswa> findByStatusAndIdProdi(StatusRecord statusRecord, Prodi prodi);
    Page<Mahasiswa> findByAngkatanAndIdProdiAndStatus(String angkatan, Prodi prodi, StatusRecord statusRecord, Pageable page);

    List<Mahasiswa> findByStatusAndAngkatanAndIdProdiAndIdProgram(StatusRecord aktif, String angkatan, Prodi prodi, Program program);
    List<Mahasiswa> findByIdProdiAndStatus(Prodi prodi, StatusRecord statusRecord);
    List<Mahasiswa> findByIdProdiAndIdProgramAndAngkatanAndStatusAktifAndStatusAndBeasiswaIsNull(Prodi prodi, Program program, String angkatan, String s, StatusRecord statusRecord);
    List<Mahasiswa> findByIdProdiAndIdProgramAndAngkatanAndStatusAndStatusAktifAndBeasiswaIsNotNull(Prodi prodi, Program program, String angkatan, StatusRecord statusRecord, String status);


    @Query(value = "select a.angkatan,c.nama_jenjang as namaJenjang from mahasiswa as a\n" +
            "inner join prodi as b on a.id_prodi = b.id\n" +
            "inner join jenjang as c on b.id_jenjang = c.id \n" +
            "where a.status = 'AKTIF' and a.status_aktif = 'AKTIF' group by a.angkatan order by a.angkatan\n", nativeQuery = true)
    List<ListAngkatanDto> listAngkatanMahasiswa();

    List<Mahasiswa> findByStatusAndStatusAktifAndBeasiswaIsNull(StatusRecord statusRecord, String status);

}
