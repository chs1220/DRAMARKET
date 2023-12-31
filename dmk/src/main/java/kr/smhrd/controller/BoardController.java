package kr.smhrd.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.imgscalr.Scalr;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.mysql.cj.util.Util;

import kr.smhrd.entity.Criteria;
import kr.smhrd.entity.PageMaker;
import kr.smhrd.entity.t_board;
import kr.smhrd.entity.t_comment;
import kr.smhrd.entity.t_member;
import kr.smhrd.mapper.Mapper;
import kr.smhrd.util.common_util;

@Controller
public class BoardController { //DAO 대신 mapper 호출
	
	@Autowired 
	private Mapper mapper;
	
	@RequestMapping("/boardList.do") //게시판 페이지
	public String boardList(Criteria cri, Model model) {
		
		List<t_board> list = mapper.boardList(cri);
		model.addAttribute("list",list);
		PageMaker pm = new PageMaker();
		pm.setCri(cri);
		pm.setTotalCount(mapper.totalCount());
		model.addAttribute("pm", pm);
		
		return "boardList";
	}
	
	@GetMapping("/boardInsert.do") //글쓰기 페이지 불러오기 
	public String boardInsert() {
		
		return "boardInsert";
	}

	
	//글 + 첨부파일 insert
	@RequestMapping(value = "/writeaddfile.do", method= RequestMethod.POST)
	public String writeaddfile(t_board vo) {
		System.out.println("1번) 이미지는 : "+vo.getImg_name());
		String id=vo.getId();
		t_member writerVO=mapper.memberById(id);  //작성자 vo 가져오기
		System.out.println(writerVO.getNick());
		vo.setNick(writerVO.getNick());  // 게시글에 작성자 닉네임 추가 
		vo.setProfile_name(writerVO.getProfile_name());  // 게시글에 작성자 프로필 이미지 패스 추가
		
		vo.setIndate(java.sql.Timestamp.valueOf(LocalDateTime.now()));
		System.out.println("2번)이미지는 : "+vo.getImg_name());
		mapper.insertWriteaddfile(vo);
		
		return "redirect:/boardList.do";
		
	}	
	
	
	//글만 insert
	@RequestMapping(value = "/write.do", method= RequestMethod.POST)
	public String write(t_board vo) {

		System.out.println(vo.getId());
		String id=vo.getId();
		
		t_member writerVO=mapper.memberById(id);  //작성자 vo 가져오기
		System.out.println(writerVO.getNick());
		vo.setNick(writerVO.getNick());  // 게시글에 작성자 닉네임 추가 
		vo.setProfile_name(writerVO.getProfile_name());  // 게시글에 작성자 프로필 이미지 패스 추가
		
		vo.setIndate(java.sql.Timestamp.valueOf(LocalDateTime.now()));
		mapper.insertWrite(vo);
		
		return "redirect:/boardList.do";
		
	}
	
	//글 등록 전 파일 등록
	@RequestMapping("/fileupload.do") 
	public String fileupload(MultipartHttpServletRequest multipartRequest,  HttpServletResponse response)
	throws ServletException, IOException{
		
		// 파일 저장 디렉토리 설정
		String uploadFolder = "C:\\Users\\smhrd\\git\\DRAMARKET\\dmk\\src\\main\\webapp\\resources\\img\\boardimg";
		//String path = multipartRequest.getServletContext().getRealPath("")+File.separator+"img"; // 쌤 경로
	    // 날짜별로 디렉토리 생성, uploadPath 설정
	    File uploadPath = new File(uploadFolder);
	    if(!uploadPath.exists()) uploadPath.mkdirs();
		

		Iterator<String> it= multipartRequest.getFileNames();
		List<String> fileList = new ArrayList<String>();
		String UploadName="";
		while (it.hasNext()) { // 마지막 파라메터가 없으면 false 반복 종료
			String paramFileName = it.next();
			
			//파일을 다루는 클래스 (파라메터로 받아온 파일의 이름, 타입, 크기 등의 정보 )
			MultipartFile mFile = multipartRequest.getFile(paramFileName);
			String fileRealName = mFile.getOriginalFilename();//실제 파일 이름
			System.out.println(fileRealName); //넘어오는 값들 확인 
			fileList.add(fileRealName);
			
			
			//업로드 이름(UploadName) 지정
			UUID uuid = UUID.randomUUID();
	        UploadName= uuid.toString() + "_" + fileRealName;
			
	        //파일 저장(UploadPath, UploadName)
	        File saveFile = new File(uploadPath, UploadName);
	        
			try {
				mFile.transferTo(saveFile);
				System.out.println("저장완료. 저장 경로는 : "+saveFile.getPath() );
				
			} catch (Exception error) {
				System.out.println(error.getMessage());
			}
			
			new common_util().thumb(saveFile, uploadPath, UploadName, fileRealName);
		}
		System.out.println(UploadName);
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().print(UploadName);
		
		return null;

		
	}
	
	//게시글 조회 페이지 불러오기 
	@RequestMapping("/boardContent.do") 
	public String boardContent(long num,Model model) {
		
		// 조회수 누적
		mapper.countUpdate(num); 
		t_board board_vo = mapper.selectContent(num);
		System.out.println("이미지 파일:"+board_vo.getImg_name());
		model.addAttribute("board_vo",board_vo);
		
		return "boardContent";
	}
	
	//댓글 달기
	@PostMapping("/commentInsert.do")
	@ResponseBody
	public String commentInsert(t_comment cmt_vo) {
		cmt_vo.setIndate(java.sql.Timestamp.valueOf(LocalDateTime.now()));
		System.out.println("댓글 입력 id는 : "+cmt_vo.getCmt_id());
		t_member mvo=mapper.memberById(cmt_vo.getCmt_id());
		cmt_vo.setNick(mvo.getNick());
		cmt_vo.setProfile_name(mvo.getProfile_name());
		System.out.println("댓글 입력 프로필 경로는 : "+cmt_vo.getProfile_name());
		mapper.cmtinsert(cmt_vo);
		
		return null;
	}
	
	//댓글 불러오기
    @ResponseBody  // ResponseBody : return 의 string 이 .jsp를 반환하지 않고, 문자열 그 자체를 반환
    @RequestMapping(value="/commentList.do", method = RequestMethod.POST)
    public List<t_comment> commentList(@RequestBody Map<String, Object> request) throws Exception{
    	
    	 long board_num = Long.parseLong((String) request.get("board_num"));
    	 System.out.println("ajax 요청 도착 : "+ board_num);
    	 List<t_comment> commentList = mapper.commentselect(board_num);
    	 System.out.println("댓글 uuid : "+commentList.get(0).getCmt_num());
    	 System.out.println("댓글 내용 : "+commentList.get(0).getCmt());
    	 
        
         return commentList;
        
    }
    
    
    
    @ResponseBody
    @RequestMapping(value="/commentDelete.do", method=RequestMethod.GET)
    public String commentDelete(@RequestParam("cmt_num") long cmt_num) {
        int result =mapper.cmtDelete(cmt_num);
        String message=null;
        if(result==1) {
            message = "success";
        } else {
            message ="fail";
        }
        return message;
    }
    
    
    

}
