package id.ac.tazkia.smilemahasiswa.controller;


import com.sun.org.apache.xpath.internal.operations.Mod;
import id.ac.tazkia.smilemahasiswa.dao.*;
import id.ac.tazkia.smilemahasiswa.dto.report.RekapDetailDosenDto;
import id.ac.tazkia.smilemahasiswa.dto.report.RekapDosenDto;
import id.ac.tazkia.smilemahasiswa.dto.report.RekapJadwalDosenDto;
import id.ac.tazkia.smilemahasiswa.dto.report.RekapSksDosenDto;
import id.ac.tazkia.smilemahasiswa.dto.schedule.ScheduleDto;
import id.ac.tazkia.smilemahasiswa.entity.*;
import id.ac.tazkia.smilemahasiswa.service.CurrentUserService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Controller
public class ReportController {
    @Autowired
    private JadwalDosenDao jadwalDosenDao;

    @Autowired
    private JadwalDao jadwalDao;

    @Autowired
    private CutiDao cutiDao;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private KaryawanDao karyawanDao;

    @Autowired
    private DosenDao dosenDao;

    @Autowired
    private TahunAkademikDao tahunAkademikDao;

    @Autowired
    private KrsDetailDao krsDetailDao;

    @Autowired
    private KrsDao krsDao;

    @Autowired
    private KelasMahasiswaDao kelasMahasiswaDao;

    @Autowired
    private MahasiswaDao mahasiswaDao;

    @Autowired
    private ProdiDao prodiDao;

    @Autowired
    private EdomQuestionDao edomQuestionDao;

    @Autowired
    private PresensiMahasiswaDao presensiMahasiswaDao;

    @Autowired
    private SoalDao soalDao;

    @Value("${upload.soal}")
    private String uploadFolder;

    @ModelAttribute("tahunAkademik")
    public Iterable<TahunAkademik> tahunAkademik() {
        return tahunAkademikDao.findByStatusNotInOrderByTahunDesc(Arrays.asList(StatusRecord.HAPUS));
    }

    @ModelAttribute("angkatan")
    public Iterable<Mahasiswa> angkatan() {
        return mahasiswaDao.cariAngkatan();
    }

    @ModelAttribute("prodi")
    public Iterable<Prodi> prodi() {
        return prodiDao.findByStatusNotIn(Arrays.asList(StatusRecord.HAPUS));
    }


    @GetMapping("/report/recapitulation/lecturer")
    public void rekapDosen(Model model, @RequestParam(required = false) TahunAkademik ta, @PageableDefault(size = Integer.MAX_VALUE) Pageable page){
        model.addAttribute("selectedTahun", ta);

        Page<RekapJadwalDosenDto> rekap = jadwalDosenDao
                .rekapJadwalDosen(StatusJadwalDosen.PENGAMPU, ta, StatusRecord.AKTIF, page);

        Map<String, RekapSksDosenDto> rekapJumlahSks = new LinkedHashMap<>();
        Map<String, List<RekapJadwalDosenDto>> detailJadwalPerDosen = new LinkedHashMap<>();

        for (RekapJadwalDosenDto r : rekap.getContent()) {

            // hitung total sks
            RekapSksDosenDto rsks = rekapJumlahSks.get(r.getIdDosen());
            if (rsks == null) {
                rsks = new RekapSksDosenDto();
                rsks.setNamaDosen(r.getNamaDosen());
                rsks.setIdDosen(r.getIdDosen());
                rsks.setTotalSks(0);
            }

            rsks.tambahSks(r.getSks());
            rekapJumlahSks.put(r.getIdDosen(), rsks);

            // jadwal per dosen
            List<RekapJadwalDosenDto> rd = detailJadwalPerDosen.get(r.getIdDosen());
            if (rd == null) {
                rd = new ArrayList<>();
                detailJadwalPerDosen.put(r.getIdDosen(), rd);
            }

            rd.add(r);
        }

        model.addAttribute("rekapJumlahSks", rekapJumlahSks);
        model.addAttribute("rekapJadwalDosen", rekap);
        model.addAttribute("rekapJadwalPerDosen", detailJadwalPerDosen);

    }

    @GetMapping("/report/recapitulation/salary")
    public void rekapGajiDosen(Model model, @RequestParam(required = false) Integer tahun,@RequestParam(required = false) Integer bulan, @PageableDefault(size = Integer.MAX_VALUE) Pageable page){

        List<RekapDosenDto> rekap = jadwalDosenDao
                .rekapDosen(tahun,bulan);
        model.addAttribute("selectedTahun", tahun);
        model.addAttribute("selectedBulan", bulan);

        Map<String, RekapDetailDosenDto> rekapJumlahSks = new LinkedHashMap<>();
        Map<String, List<RekapDosenDto>> detailJadwalPerDosen = new LinkedHashMap<>();

        for (RekapDosenDto r : rekap) {

            // hitung total sks
            RekapDetailDosenDto rsks = rekapJumlahSks.get(r.getId());
            if (rsks == null) {
                rsks = new RekapDetailDosenDto();
                rsks.setNamaDosen(r.getNama());
                rsks.setIdDosen(r.getId());
                rsks.setSks1(r.getSks());
                rsks.setJumlah(r.getHadir());

            }

            rsks.tambahSks(r.getSks());
            rekapJumlahSks.put(r.getId(), rsks);

            // jadwal per dosen
            List<RekapDosenDto> rd = detailJadwalPerDosen.get(r.getId());
            if (rd == null) {
                rd = new ArrayList<>();
                detailJadwalPerDosen.put(r.getId(), rd);
            }

            rd.add(r);
        }

        model.addAttribute("rekapJumlahSks", rekapJumlahSks);
        model.addAttribute("rekapJadwalDosen", rekap);
        model.addAttribute("rekapJadwalPerDosen", detailJadwalPerDosen);

    }

    @GetMapping("/report/recapitulation/ipk")
    public void rekapSks(Model model,@RequestParam(required = false) TahunAkademik tahunAkademik,
                         @RequestParam(required = false) String angkatan){

        if (tahunAkademik != null) {
            model.addAttribute("selectedAngkatan", angkatan);
            model.addAttribute("selectedTahun", tahunAkademik);
            model.addAttribute("ipk", krsDetailDao.cariIpk(tahunAkademik,angkatan));
        }
    }

    @GetMapping("/report/recapitulation/downloadipk")
    public void listPerMatkul(@RequestParam(required = false) TahunAkademik tahunAkademik,
                              @RequestParam(required = false) String angkatan, HttpServletResponse response) throws IOException {

        List<Object[]> listDownload = krsDetailDao.cariIpk(tahunAkademik,angkatan);

        String[] columns = {"No", "Nim", "Nama", "Prodi", "Tahun Akademik", "SKS Semester", "SKS Total", "IP Semester", "IPK"};

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("List Ipk Angkatan " + angkatan);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.BLACK.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);


        Row headerRow = sheet.createRow(2);

        for (int i = 0; i < columns.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerCellStyle);
        }

        int rowNum = 3;
        int baris = 1;

        for (Object[] list : listDownload){
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(baris++);
            row.createCell(1).setCellValue(list[0].toString());
            row.createCell(2).setCellValue(list[1].toString());
            row.createCell(3).setCellValue(list[2].toString());
            row.createCell(4).setCellValue(tahunAkademik.getNamaTahunAkademik());
            row.createCell(5).setCellValue(list[5].toString());
            row.createCell(6).setCellValue(list[7].toString());
            row.createCell(7).setCellValue(list[6].toString());
            row.createCell(8).setCellValue(list[8].toString());
        }

        for (int i = 0; i < columns.length; i++){
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=List Ipk Tahun Akademik-"+tahunAkademik.getNamaTahunAkademik()+"-"+"Angkatan " + angkatan+".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();

    }

    @GetMapping("/report/recapitulation/edom")
    public void rekapEdom(Model model,@RequestParam(required = false) TahunAkademik tahunAkademik,
                          @RequestParam(required = false) Prodi prodi){

        if (tahunAkademik != null){
            List<EdomQuestion> edomQuestion = edomQuestionDao.findByStatusAndTahunAkademikOrderByNomorAsc(StatusRecord.AKTIF,tahunAkademik);
            if (edomQuestion != null){
                EdomQuestion edomQuestion1 = edomQuestionDao.findByStatusAndNomorAndTahunAkademik(StatusRecord.AKTIF,1,tahunAkademik);
                EdomQuestion edomQuestion2 = edomQuestionDao.findByStatusAndNomorAndTahunAkademik(StatusRecord.AKTIF,2,tahunAkademik);
                EdomQuestion edomQuestion3 = edomQuestionDao.findByStatusAndNomorAndTahunAkademik(StatusRecord.AKTIF,3,tahunAkademik);
                EdomQuestion edomQuestion4 = edomQuestionDao.findByStatusAndNomorAndTahunAkademik(StatusRecord.AKTIF,4,tahunAkademik);
                EdomQuestion edomQuestion5 = edomQuestionDao.findByStatusAndNomorAndTahunAkademik(StatusRecord.AKTIF,5,tahunAkademik);
                model.addAttribute("edomQuestion1",edomQuestion1);
                model.addAttribute("edomQuestion2",edomQuestion2);
                model.addAttribute("edomQuestion3",edomQuestion3);
                model.addAttribute("edomQuestion4",edomQuestion4);
                model.addAttribute("edomQuestion5",edomQuestion5);
            }else {
                model.addAttribute("questionNull", "Pertanyaan edom untuk tahun akademik ini belum dibuat");
            }
            model.addAttribute("selectedTahun", tahunAkademik);
            model.addAttribute("selectedProdi", prodi);
            model.addAttribute("rekapEdom",krsDetailDao.rekapEdom(tahunAkademik,prodi));
        }

    }

    @GetMapping("/report/recapitulation/bkd")
    public void rekapBkdDosen(Model model, Authentication authentication, @RequestParam(required = false)TahunAkademik tahunAkademik){
        User user = currentUserService.currentUser(authentication);
        Karyawan karyawan = karyawanDao.findByIdUser(user);
        Dosen dosen = dosenDao.findByKaryawan(karyawan);

        if (tahunAkademik != null){
            model.addAttribute("selectedTahun", tahunAkademik);

            List<ScheduleDto> jadwal = jadwalDao.lecturerAssesment(dosen,StatusRecord.AKTIF, tahunAkademik);
            model.addAttribute("jadwal", jadwal);
        }


    }

    @GetMapping("/report/historymahasiswa")
    public void historyMahasiswa(Model model, @RequestParam(required = false)String nim){
        if (nim != null) {
            model.addAttribute("nim", nim);
            Mahasiswa mahasiswa = mahasiswaDao.findByNim(nim);
            String URL =  "https://api.whatsapp.com/send?phone=" + mahasiswa.getTeleponSeluler();
            String urlProdi = "https://smile.tazkia.ac.id/mahasiswa/form?mahasiswa=" + mahasiswa.getNim();
            KelasMahasiswa kelasMahasiswa = kelasMahasiswaDao.findByMahasiswaAndStatus(mahasiswa, StatusRecord.AKTIF);

            model.addAttribute("mhs",mahasiswa);
            model.addAttribute("kelas", kelasMahasiswa);
            model.addAttribute("url", URL);
            model.addAttribute("urlProdi", urlProdi);
            model.addAttribute("history", krsDetailDao.historyMahasiswa(mahasiswa));
            model.addAttribute("ipk", krsDetailDao.ipk(mahasiswa));
            model.addAttribute("sksTotal", krsDetailDao.totalSks(mahasiswa));
            model.addAttribute("semester", krsDetailDao.semesterHistory(mahasiswa));
            model.addAttribute("khsHistory", krsDetailDao.khsHistoty(mahasiswa));
            model.addAttribute("transkrip", krsDetailDao.transkrip(mahasiswa));
            model.addAttribute("semesterTranskript", krsDao.semesterTranskript(mahasiswa.getId()));
            model.addAttribute("transkriptTampil", krsDetailDao.transkriptTampil(mahasiswa.getId()));
        }
    }

    @GetMapping("/report/cuti")
    public void mahasiswaCuti(Model model,@PageableDefault(size = 10)Pageable pageable){
        model.addAttribute("listCutiMahasiswa",cutiDao.findByStatusOrderByStatusPengajuaanDesc(StatusRecord.AKTIF, pageable));
    }

    @PostMapping("/proses/cuti")
    public String prosesCuti(@Valid Cuti cuti, @RequestParam Mahasiswa mahasiswa){
        cuti.setMahasiswa(mahasiswa);
        cuti.setTanggalPengajuaan(LocalDate.now());
        cuti.setStatusPengajuaan("DIAJUKAN");
        cutiDao.save(cuti);

        return "redirect:/report/cuti";
    }

    @GetMapping("/report/recapitulation/nilai")
    public void nilai(Model model,@RequestParam Jadwal jadwal){
        String tahun = jadwal.getTahunAkademik().getNamaTahunAkademik().substring(0, 9);

        model.addAttribute("tahun", tahun);
        model.addAttribute("jadwal", jadwal);
        model.addAttribute("dosen", jadwalDosenDao.headerJadwal(jadwal.getId()));
        model.addAttribute("nilai", presensiMahasiswaDao.bkdNilai(jadwal));
    }

    @GetMapping("/report/recapitulation/attendance")
    public void attendance(Model model,@RequestParam Jadwal jadwal){
        String tahun = jadwal.getTahunAkademik().getNamaTahunAkademik().substring(0, 9);

        model.addAttribute("tahun", tahun);

        model.addAttribute("jadwal", jadwal);

        model.addAttribute("dosen", jadwalDosenDao.headerJadwal(jadwal.getId()));

        List<Object[]> hasil = presensiMahasiswaDao.bkdAttendance(jadwal);

        model.addAttribute("attendance", hasil);


    }

    @GetMapping("/report/fileberkas")
    public void fileBerkas(Model model, @RequestParam(required = false) TahunAkademik tahunAkademik, Authentication authentication){
        User user = currentUserService.currentUser(authentication);
        Karyawan karyawan = karyawanDao.findByIdUser(user);
        Dosen dosen = dosenDao.findByKaryawan(karyawan);
        if (tahunAkademik != null){
            model.addAttribute("selectedTahun", tahunAkademik);
            model.addAttribute("fileBerkas", jadwalDao.findByTahunAkademikAndDosenAndStatus(tahunAkademik, dosen, StatusRecord.AKTIF));
        }
    }

    @RequestMapping("/download/")
    public void downloadBerkas(HttpServletRequest request, HttpServletResponse response, @RequestParam Jadwal jadwal, @RequestParam String status){

        if (status == "UAS"){
            Soal soal = soalDao.findByJadwalAndStatusAndStatusApproveAndStatusSoal(jadwal, StatusRecord.AKTIF, StatusApprove.APPROVED, StatusRecord.UAS);
            String fileName = soal.getFileApprove();
            String lokasi = uploadFolder + File.separator + soal.getJadwal().getId();

            Path file = Paths.get(lokasi, fileName);
            if (Files.exists(file))
            {
                response.setContentType("application/application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.addHeader("Content-Disposition", "attachment; filename="+fileName);
                try
                {
                    Files.copy(file, response.getOutputStream());
                    response.getOutputStream().flush();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }else if (status == "UTS"){
            Soal soal = soalDao.findByJadwalAndStatusAndStatusApproveAndStatusSoal(jadwal, StatusRecord.AKTIF, StatusApprove.APPROVED, StatusRecord.UTS);
            String fileName = soal.getFileApprove();
            String lokasi = uploadFolder + File.separator + soal.getJadwal().getId();

            Path file = Paths.get(lokasi, fileName);
            if (Files.exists(file))
            {
                response.setContentType("application/application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.addHeader("Content-Disposition", "attachment; filename="+fileName);
                try
                {
                    Files.copy(file, response.getOutputStream());
                    response.getOutputStream().flush();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }


    }
}
