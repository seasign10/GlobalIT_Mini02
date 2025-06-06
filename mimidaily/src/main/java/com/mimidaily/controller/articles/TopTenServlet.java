package com.mimidaily.controller.articles;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mimidaily.dao.ArticlesDAO;
import com.mimidaily.dto.ArticlesDTO;

@WebServlet("/articles/topten.do")
public class TopTenServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public TopTenServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// DAO 생성
		ArticlesDAO dao = new ArticlesDAO();
		// 게시글 상위 10개 조회
		List<ArticlesDTO> ttArticles = dao.selectTopArticles();  // DAO에서 상위 10개만 조회
//		for (ArticlesDTO dto : ttArticles) {
//		    System.out.println("---- 게시글 ----");
//		    System.out.println("제목: " + dto.getTitle());
//			if(dto.getCategory() == 1) System.out.println("카테고리: 여행");
//			if(dto.getCategory() == 2) System.out.println("카테고리: 맛집");
//		    System.out.println("해시태그: " + dto.getHashtags());
//		}
		// DB 연결 닫기
		dao.close();
		// request 영역에 저장 후 JSP로 포워드
		request.setAttribute("topTenArticles", ttArticles);
		request.getRequestDispatcher("/articles/topten.jsp").forward(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
}