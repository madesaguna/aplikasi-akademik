package id.ac.tazkia.smilemahasiswa.controller;

import id.ac.tazkia.smilemahasiswa.dao.*;
import id.ac.tazkia.smilemahasiswa.dto.graduation.WisudaDto;
import id.ac.tazkia.smilemahasiswa.entity.*;
import id.ac.tazkia.smilemahasiswa.service.CurrentUserService;
import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Controller
public class TugasAkhirController {

    @Autowired
    PeriodeWisudaDao periodeWisudaDao;

    @Autowired
    KategoriTugasAkhirDao kategoriDao;

    @Autowired
    private SidangDao sidangDao;

    @Autowired
    WisudaDao wisudaDao;

    @Autowired
    ProdiDao prodiDao;

    @Autowired
    private MahasiswaDao mahasiswaDao;

    @Autowired
    private AyahDao ayahDao;

    @Autowired
    private IbuDao ibuDao;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private BeasiswaDao beasiswaDao;

    @Value("${upload.wisuda}")
    private String wisudaFolder;

    @ModelAttribute("prodi")
    public Iterable<Prodi> prodi() {
        return prodiDao.findAll();
    }

    // PERIODE WISUDA

    @GetMapping("/graduation/periodeWisuda/list")
    public void listPeriode(Model model, @PageableDefault(size = 10) Pageable page, String search){

        if (StringUtils.hasText(search)){
            model.addAttribute("search", search);
            model.addAttribute("listPeriode", periodeWisudaDao.findByStatusNotInAndNamaContainingIgnoreCase(Arrays.asList(StatusRecord.HAPUS), search, page));
        }else{
            model.addAttribute("listPeriode", periodeWisudaDao.findByStatusNotIn(Arrays.asList(StatusRecord.HAPUS), page));
        }
    }

    @GetMapping("/graduation/periodeWisuda/form")
    public void formPeriode(Model model, @RequestParam(required = false) String id){
        model.addAttribute("newPeriode", new PeriodeWisuda());

        if (id != null && !id.isEmpty()){
            PeriodeWisuda periodeWisuda = periodeWisudaDao.findById(id).get();
            if (periodeWisuda != null){
                model.addAttribute("newPeriode", periodeWisuda);
                if (periodeWisuda.getStatus() == null){
                    periodeWisuda.setStatus(StatusRecord.NONAKTIF);
                }
            }
        }

    }

    @PostMapping("/graduation/periodeWisuda/new")
    public String inputPeriode(@Valid PeriodeWisuda wisuda){

        periodeWisudaDao.save(wisuda);
        return "redirect:list";

    }

    @PostMapping("/graduation/periodeWisuda/delete")
    public String deletePeriode(@RequestParam PeriodeWisuda wisuda){

        wisuda.setStatus(StatusRecord.HAPUS);
        periodeWisudaDao.save(wisuda);
        return "redirect:list";

    }

    // KETEGORI TUGAS AKHIR

    @GetMapping("/graduation/kategori/list")
    public void listKategori(Model model, @RequestParam(required = false) Prodi prodi, @PageableDefault(size = 10) Pageable page){

        model.addAttribute("selectProdi", prodi);
        model.addAttribute("prodi", prodiDao.findByStatus(StatusRecord.AKTIF));
        model.addAttribute("listNote", kategoriDao.findByProdiAndJenisAndStatusNotIn(prodi, Jenis.NOTE, Arrays.asList(StatusRecord.HAPUS), page));
        model.addAttribute("listSeminar", kategoriDao.findByProdiAndJenisAndStatusNotIn(prodi, Jenis.SEMINAR, Arrays.asList(StatusRecord.HAPUS), page));
        model.addAttribute("listSidang", kategoriDao.findByProdiAndJenisAndStatusNotIn(prodi, Jenis.SIDANG, Arrays.asList(StatusRecord.HAPUS), page));

        // modal add kategori
        model.addAttribute("jenis", Jenis.values());
        model.addAttribute("jenisValidasi", JenisValidasi.values());

    }

    @PostMapping("/graduation/kategori/new")
    public String saveKategori(@RequestParam(required = false) String[] prodi, @RequestParam(required = false) String nama,
                               @RequestParam(required = false) Jenis jenis, @RequestParam(required = false) JenisValidasi jenisValidasi){

        for (String p : prodi){
            Prodi prod = prodiDao.findById(p).get();
            KategoriTugasAkhir kategori = new KategoriTugasAkhir();
            kategori.setNama(nama);
            kategori.setProdi(prod);
            kategori.setJenis(jenis);
            kategori.setJenisValidasi(jenisValidasi);
            kategori.setStatus(StatusRecord.AKTIF);
            kategoriDao.save(kategori);
        }

        return "redirect:list";

    }
    @GetMapping("/graduation/sidang/mahasiswa/wisuda")
    public void daftarWisuda(Model model,@RequestParam Sidang sidang, Authentication authentication){
        User user = currentUserService.currentUser(authentication);
        Mahasiswa mahasiswa = mahasiswaDao.findByUser(user);
        model.addAttribute("mahasiswa", mahasiswa);
        model.addAttribute("sidang", sidang);
        model.addAttribute("beasiswa", beasiswaDao.findByStatusOrderByNamaBeasiswa(StatusRecord.AKTIF));
        WisudaDto wisudaDto = new WisudaDto();
        wisudaDto.setMahasiswa(mahasiswa.getId());
        wisudaDto.setNama(mahasiswa.getNama());
        wisudaDto.setTanggal(mahasiswa.getTanggalLahir());
        wisudaDto.setKelamin(mahasiswa.getJenisKelamin().toString());
        wisudaDto.setAyah(mahasiswa.getAyah().getNamaAyah());
        wisudaDto.setSidang(sidang.getId());
        wisudaDto.setIbu(mahasiswa.getIbu().getNamaIbuKandung());
        wisudaDto.setToga(mahasiswa.getUkuranBaju());
        if (mahasiswa.getBeasiswa() != null) {
            wisudaDto.setBeasiswa(mahasiswa.getBeasiswa().getNamaBeasiswa());
            wisudaDto.setIdBeasiswa(mahasiswa.getBeasiswa().getId());
        }
        wisudaDto.setJudulIndo(sidang.getJudulTugasAkhir());
        wisudaDto.setJudulInggris(sidang.getJudulInggris());
        model.addAttribute("wisuda", wisudaDto);

    }

    @PostMapping("/graduation/sidang/mahasiswa/wisuda-post")
    public String daftarWisuda(@Valid WisudaDto wisudaDto, MultipartFile foto, RedirectAttributes attributes, Authentication authentication) throws Exception{
        User user = currentUserService.currentUser(authentication);
        Mahasiswa mahasiswa = mahasiswaDao.findByUser(user);
        String namaAsli = foto.getOriginalFilename();
        Long ukuran = foto.getSize();

        Ayah ayah = ayahDao.findById(mahasiswa.getAyah().getId()).get();
        Ibu ibu = ibuDao.findById(mahasiswa.getIbu().getId()).get();
        Sidang sidang = sidangDao.findById(wisudaDto.getSidang()).get();

        sidang.setJudulTugasAkhir(wisudaDto.getJudulIndo());
        sidang.setJudulInggris(wisudaDto.getJudulInggris());
        sidangDao.save(sidang);

        mahasiswa.setNama(WordUtils.capitalize(wisudaDto.getNama()));
        mahasiswa.setTanggalLahir(wisudaDto.getTanggal());
        mahasiswa.setJenisKelamin(JenisKelamin.valueOf(wisudaDto.getKelamin()));
        mahasiswa.setUkuranBaju(wisudaDto.getToga());
        mahasiswaDao.save(mahasiswa);


        ayah.setNamaAyah(wisudaDto.getAyah());
        ibu.setNamaIbuKandung(wisudaDto.getNama());
        ayahDao.save(ayah);
        ibuDao.save(ibu);

        if (ukuran >= 1000000){
            String extension = "";

            int i = namaAsli.lastIndexOf('.');
            int p = Math.max(namaAsli.lastIndexOf('/'), namaAsli.lastIndexOf('\\'));

            if (i > p) {
                extension = namaAsli.substring(i + 1);
            }


            String namaFile = mahasiswa.getNim() + "_" + mahasiswa.getNama();
            String lokasiUpload = wisudaFolder + File.separator + mahasiswa.getNim();
            new File(lokasiUpload).mkdirs();
            File tujuan = new File(lokasiUpload + File.separator + namaFile + "." + extension);
            foto.transferTo(tujuan);

            PeriodeWisuda periodeWisuda = periodeWisudaDao.findByStatus(StatusRecord.AKTIF);

            Wisuda wisuda = new Wisuda();
            wisuda.setMahasiswa(mahasiswa);
            wisuda.setFoto(namaFile + "." + extension);
            wisuda.setSidang(sidang);
            wisuda.setPeriodeWisuda(periodeWisuda);
            wisuda.setUkuran(String.valueOf(ukuran / (1024 * 1024)) + " Mb");
            wisuda.setStatus(StatusApprove.WAITING);
            wisudaDao.save(wisuda);
            return "redirect:waiting";


        }else {
            attributes.addFlashAttribute("kurang", "Ukuran File yg diupload kurang dari 1MB");
            return "redirect:wisuda?sidang="+wisudaDto.getSidang();

        }


    }

    @GetMapping("/graduation/sidang/mahasiswa/result")
    public void resultWisuda(Model model,@RequestParam Wisuda wisuda, Authentication authentication){

        model.addAttribute("approved", wisuda);

    }

    @GetMapping("/graduation/sidang/mahasiswa/waiting")
    public void waitingWisuda(Model model, Authentication authentication){
        User user = currentUserService.currentUser(authentication);
        Mahasiswa mahasiswa = mahasiswaDao.findByUser(user);

        Wisuda waiting = wisudaDao.findByMahasiswaAndStatus(mahasiswa,StatusApprove.WAITING);
        if (waiting != null){
            model.addAttribute("waiting", waiting);
        }else {
            model.addAttribute("data", wisudaDao.findFirstByMahasiswaAndStatus(mahasiswa, StatusApprove.REJECTED));
            model.addAttribute("reject", "reject");
        }

    }

    @GetMapping("/graduation/sidang/mahasiswa/valid")
    public String resultWisuda(Model model,@RequestParam(name = "id", value = "id") Mahasiswa mahasiswa, Authentication authentication){
        Wisuda wisuda = wisudaDao.findByMahasiswaAndStatus(mahasiswa,StatusApprove.APPROVED);

        if (wisuda != null){
            return "redirect:result?wisuda=" + wisuda.getId();
        }else {
            return "redirect:waiting";
        }

    }

    @GetMapping("/graduation/sidang/admin/listwisuda")
    public void resultWisuda(Model model,@RequestParam(required = false) Prodi prodi,@RequestParam(required = false) PeriodeWisuda periode, Pageable page){
        model.addAttribute("periode", periodeWisudaDao.findByStatusNotInOrderByTanggalWisudaDesc(Arrays.asList(StatusRecord.HAPUS)));
        model.addAttribute("selectedProdi",prodi);
        model.addAttribute("selectedPeriode",periode);

        if (prodi == null){
            model.addAttribute("list", wisudaDao.findByPeriodeWisudaAndStatusNotIn(periode,Arrays.asList(StatusApprove.REJECTED,StatusApprove.HAPUS), page));
        }else {
            model.addAttribute("list", wisudaDao.findByPeriodeWisudaAndMahasiswaIdProdiAndStatusNotInOrderByStatusDescMahasiswaIdProdiAsc(periode,prodi,Arrays.asList(StatusApprove.REJECTED,StatusApprove.HAPUS),page));
        }


    }

    @GetMapping("/graduation/sidang/admin/wisuda")
    public void detailWisuda(Model model, @RequestParam(name = "id", value = "id") Wisuda wisuda){
        model.addAttribute("wisuda", wisuda);
    }

    @GetMapping("/graduation/sidang/admin/approve")
    public String approveWisuda(@RequestParam Wisuda wisuda){
        wisuda.setStatus(StatusApprove.APPROVED);
        wisudaDao.save(wisuda);

        return "redirect:listwisuda?periode="+ wisuda.getPeriodeWisuda().getId();
    }

    @GetMapping("/graduation/sidang/admin/reject")
    public String rejectWisuda(@RequestParam Wisuda wisuda){
        wisuda.setStatus(StatusApprove.REJECTED);
        wisudaDao.save(wisuda);

        return "redirect:listwisuda?periode="+ wisuda.getPeriodeWisuda().getId();
    }

    @GetMapping("/upload/{wisuda}/fotowisuda/")
    public ResponseEntity<byte[]> fotoWisuda(@PathVariable Wisuda wisuda, Model model) throws Exception {
        String lokasiFile = wisudaFolder + File.separator + wisuda.getMahasiswa().getNim()
                + File.separator + wisuda.getFoto();

        try {
            HttpHeaders headers = new HttpHeaders();
            if (wisuda.getFoto().toLowerCase().endsWith("jpeg") || wisuda.getFoto().toLowerCase().endsWith("jpg")) {
                headers.setContentType(MediaType.IMAGE_JPEG);
            } else if (wisuda.getFoto().toLowerCase().endsWith("png")) {
                headers.setContentType(MediaType.IMAGE_PNG);
            } else if (wisuda.getFoto().toLowerCase().endsWith("pdf")) {
                headers.setContentType(MediaType.APPLICATION_PDF);
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            byte[] data = Files.readAllBytes(Paths.get(lokasiFile));
            return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
        } catch (Exception err) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();


        }

    }

    @GetMapping("/graduation/info")
    public void detailWisuda(Model model, Authentication authentication){
        User user = currentUserService.currentUser(authentication);
        Mahasiswa mahasiswa = mahasiswaDao.findByUser(user);
    }

}
