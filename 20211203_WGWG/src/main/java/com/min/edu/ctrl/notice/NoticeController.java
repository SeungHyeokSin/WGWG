package com.min.edu.ctrl.notice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

import com.min.edu.vo.notice.NoticeFileVO;
import com.min.edu.vo.notice.NoticeVO;
import com.min.edu.vo.paging.PageVO;
import com.min.edu.model.notice.INoticeService;

@Controller
public class NoticeController {	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private INoticeService service;	
	
	@RequestMapping(value ="/noticeList.do" , method = {RequestMethod.POST,RequestMethod.GET})
	  public String paging(Model model,HttpServletRequest request) {
		PageVO paging = new PageVO(
	               request.getParameter("index"),
	               request.getParameter("pageStartNum"),
	               request.getParameter("listCnt")
	            );
	      
	      paging.setTotal(service.selectTotalPaging());
	      List<NoticeVO> lists = service.selectPaging(paging);
	      
	      model.addAttribute("lists",lists);
	      model.addAttribute("paging", paging);
	      logger.info("페이징 DTO 값 :{}",paging.toString());
	      return "notice/noticeList";
	   }
	/*
	@GetMapping(value="/noticeList.do")
	public String getAllList(PageVO vo, Model model
			, @RequestParam(value="nowPage", required=false)String nowPage
			, @RequestParam(value="cntPerPage", required=false)String cntPerPage 
			, @RequestParam(value="notice_chk", required=false)String notice_chk) {
		
		logger.info("getAllList Controller 진입");
		if (nowPage == null && cntPerPage == null) {
			nowPage = "1";
			cntPerPage = "5";
		} else if (nowPage == null) {
			nowPage = "1";
		} else if (cntPerPage == null) { 
			cntPerPage = "5";
		}	
		logger.info(" notice_chk (☞ﾟヮﾟ)☞☜(ﾟヮﾟ☜)"+notice_chk);
		if(notice_chk == null ||  notice_chk.equals("모두보기")) {
			notice_chk = "모두보기";  // -->나중에 '회사'로 변경 
			int total = service.countNotice();
			vo = new PageVO(total, Integer.parseInt(nowPage), Integer.parseInt(cntPerPage),notice_chk);
			model.addAttribute("paging", vo);	
			model.addAttribute("viewAll", service.selectNotice(vo));
		}else {
			int total = service.countNoticechk(notice_chk);
			logger.info(" total (☞ﾟヮﾟ)☞☜(ﾟヮﾟ☜)"+total);
			vo = new PageVO(total, Integer.parseInt(nowPage), Integer.parseInt(cntPerPage),notice_chk);
			model.addAttribute("paging", vo);
			model.addAttribute("viewAll", service.selectNotchk(vo));
		}
		
		return "notice/notice";
	}	
	*/
	
	@GetMapping(value="/noticeInsertForm.do")
	public String noticeInsertForm() {		
		return "notice/noticeInsert";
	}
	
	@PostMapping(value="/noticeInsert.do")
	public String insertNotice(NoticeVO vo,HttpServletRequest requset ,	Model model) {
		//int cnt = service.insertNotice(vo);
		
		if(vo.getFile()!=null) {
		MultipartFile file = vo.getFile();
		
		//경로를 따로 db에 저장해주는게 좋음.
		String fileName= file.getOriginalFilename();
		vo.setFile_name(fileName);
		NoticeUtil.createUUID();
		NoticeFileVO fvo = new NoticeFileVO();
		String notice_file_save_nm = NoticeUtil.createUUID()+
				fileName.substring(fileName.indexOf("."));
		int filesize = (int) file.getSize();
		fvo.setNotice_file_nm(fileName);
		fvo.setNotice_file_save_nm(notice_file_save_nm);
		fvo.setNotice_file_size(filesize);
		System.out.println("****************"+vo);
		service.insertNotice(vo);
		service.insertFile(fvo);
		
			//물리적인 파일을 저장
			//파일을 서버 (절대경로/상대경로) IO로 업로드
			InputStream inputStream=null;//데이터가 뭐가 들어올지 모르니 (data가 이미지일지,pdf일지,동영상일지 모르니까 inputStream으로 저장)
			OutputStream outputStream=null;
			
			//io3단계 (파일을읽어옴 , 파일위치를 만듬 , 파일을 써준다)
			
			//파일을 읽음
			try {
				inputStream = file.getInputStream();  //파일을 읽어와서 10101001식으로 읽어온다. (while을 사용하면 다중파일 가능)
				
				//저장 위치를 만듬
				
				String path = WebUtils.getRealPath(requset.getSession().getServletContext(), "/storage");
				System.out.println("*******"+requset.getSession().getServletContext());
				
				//String path = "C:\\eclipse\\Spring\\20211214_Spring_File\\src\\main\\webapp\\storage"; //절대경로
				System.out.println("*******"+path);
				
				//만약 저장위치가 없다면 저장위치만들기
				File storage = new File(path);
				if(!storage.exists()) {
					storage.mkdirs();
				}
				//저장할 파일이 없다면 만들어주고 override 함
				File newfile = new File(path+"/"+fileName);
				if(!newfile.exists()) {
					newfile.createNewFile();
				}
				
				//파일을 쓸 위치를 지정해 줌
				outputStream = new FileOutputStream(newfile);
				
				//파일을 써준다.
				int read = 0;
				byte[] n = new byte[(int)file.getSize()];   //int , long
				while((read = inputStream.read(n)) != -1) {
					outputStream.write(n,0,read);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					inputStream.close();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}		
			
			
		}else {
			vo.setFile_name("파일없음");
			service.insertNotice(vo);			
		}
		
		
		
		return "redirect:/noticeList.do";
		
	}
	@RequestMapping(value ="/detailnotice.do" , method = {RequestMethod.POST,RequestMethod.GET})
	public String viewDetail(Model model, 
	                        @RequestParam("notice_no")int notice_no) {
	  NoticeVO vo =  service.getBoard(notice_no);
	  System.out.println("*****************"+vo);
	  model.addAttribute("vo",vo);

	  return "notice/detailnotice";
	}
	
	@GetMapping("/noticeupdate.do")
	public String modify(@RequestParam("notice_no")int notice_no, Model model) {
		model.addAttribute("vo", service.detailNotice(notice_no));
		return "notice/noticeupdate";
	}

	@PostMapping("/noticeupdate.do")
	public String modify(NoticeVO vo) {
		service.updateNotice(vo);
		return "redirect:/detailnotice.do?notice_no="+ vo.getNotice_no();
	}
	
	@GetMapping("/noticedelete.do")
	public String delete(@RequestParam("notice_no")int notice_no) {
		service.deleteNotice(notice_no);
		return "redirect:/noticeList.do";
	}
}
