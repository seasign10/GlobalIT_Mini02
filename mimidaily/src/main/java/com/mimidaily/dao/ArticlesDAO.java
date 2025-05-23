package com.mimidaily.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mimidaily.common.DBConnPool;
import com.mimidaily.dto.ArticlesDTO;

public class ArticlesDAO extends DBConnPool {
    public ArticlesDAO() {
        super();
    }
    
 // 실시간 관심기사 =============================================================
 	public List<ArticlesDTO> viewestList(){
 		List<ArticlesDTO> viewest=new ArrayList<ArticlesDTO>();

 		// 조회수 많은 기사 순으로 내림차순
 		String query=""
 					+"select * from (select * from articles order by visitcnt desc)"
 					+" where rownum <= 4";
 		
 		try {
 			stmt=con.createStatement();
 			rs=stmt.executeQuery(query);
 			while(rs.next()) {
 				ArticlesDTO dto=new ArticlesDTO();
 				dto.setIdx(rs.getInt(1));
                dto.setTitle(rs.getString(2));
                dto.setContent(rs.getString(3));
                dto.setCategory(rs.getInt(4));
                dto.setCreated_at(rs.getTimestamp(5));
                dto.setVisitcnt(rs.getInt(6));
                dto.setMembers_id(rs.getString(7));
                dto.setThumbnails_idx(rs.getInt(8));
                loadThumbnail(dto);
                viewest.add(dto);
 			}
 		}catch(Exception e) {e.getStackTrace();}
 		return viewest;
 	}

 	// 기사 갯수 =============================================================
    public int selectCount(Map<String, Object> map) {
        int totalCount = 0;
        String query = "SELECT COUNT(*) FROM articles";
        boolean whereAdded = false;
        
        // category 조건 추가 (예: MustEat 페이지에서 category가 2인 글만 보이도록)
        if (map.get("category") != null) {
            query += " WHERE category = " + map.get("category");
            whereAdded = true;
        }
        
        // 검색어 조건 추가
        if (map.get("searchWord") != null) {
            if (whereAdded) {
                query += " AND " + map.get("searchField") + " LIKE '%" + map.get("searchWord") + "%'";
            } else {
                query += " WHERE " + map.get("searchField") + " LIKE '%" + map.get("searchWord") + "%'";
            }
        }
        
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
            rs.next();
            totalCount = rs.getInt(1);
        } catch (Exception e) {
            System.out.println("기사 카운트 중 예외 발생");
            e.printStackTrace();
        }
        return totalCount;
    }
    

    // 목록 반환(List) =============================================================
    public List<ArticlesDTO> selectListPage(Map<String,Object> map){
    	List<ArticlesDTO> article=new ArrayList<ArticlesDTO>();
    	String query=""
    	+"select * from ("
    	+"	select Tb.*, rownum rNum from (" // Tb의 모든 칼럼, rNum: rownum의 별칭
    	+"		select * from articles";


		// 카테고리 조건
	    if (map.get("category") != null) { // (1:여행 2:맛집)
	        query += " where category = " + map.get("category");
	    }
		
	    // 검색 조건
	    if (map.get("searchWord") != null) {
	        // 카테고리 조건이 있을 경우 AND 추가
	        if (map.get("category") != null) {
	            query += " and " + map.get("searchField") + " like '%" + map.get("searchWord") + "%'";
	        } else {
	            query += " where " + map.get("searchField") + " like '%" + map.get("searchWord") + "%'";
	        }
	    }
	    
		query+="		order by idx desc"
				+"	) Tb"
				+" )"
				+" where rNum between ? and ?";
		
		try {
			psmt=con.prepareStatement(query); // 동적쿼리
			psmt.setInt(1, (Integer) map.get("start"));
			psmt.setInt(2, (Integer) map.get("end"));
			rs=psmt.executeQuery();
			while(rs.next()) {
				ArticlesDTO dto=new ArticlesDTO();
				dto.setIdx(rs.getInt(1)); // rs.get...(rs의 column row) 또는 명확한 컬럼명 넣어도 됨
	            dto.setTitle(rs.getString(2));
	            dto.setContent(rs.getString(3));
	            dto.setCategory(rs.getInt(4));
	            dto.setCreated_at(rs.getTimestamp(5));
	            dto.setVisitcnt(rs.getInt(6));
	            dto.setMembers_id(rs.getString(7));
	            dto.setThumbnails_idx(rs.getInt(8));
	             
	            // 썸네일 정보 로드
	            loadThumbnail(dto);
	             
				article.add(dto);
			}
		}catch(Exception e) {
			System.out.println("기사 목록 조회 중 예외 발생");
			e.getStackTrace();
		}
		return article;
	}
    
 	// 게시글 작성 =============================================================
    public int insertWrite(ArticlesDTO dto) {
    	  int articleId = 0;
          try {
              if (dto.getOfile() == null || dto.getOfile().trim().equals("")) {
                  // 파일 업로드가 없는 경우: articles 테이블에만 INSERT
                  String query = "INSERT INTO articles (idx, title, content, category, created_at, visitcnt, members_id, thumbnails_idx) " +
                                 "VALUES (articles_seq.NEXTVAL, ?, ?, ?, ?, 0, ?, NULL)";
                  psmt = con.prepareStatement(query);
                  psmt.setString(1, dto.getTitle());
                  psmt.setString(2, dto.getContent());
                  psmt.setInt(3, dto.getCategory());
                  psmt.setTimestamp(4, dto.getCreated_at());
                  psmt.setString(5, dto.getMembers_id());
                  int result = psmt.executeUpdate();
                  if (result > 0) {
                      stmt = con.createStatement();
                      rs = stmt.executeQuery("SELECT articles_seq.CURRVAL FROM dual");
                      if (rs.next()) {
                          articleId = rs.getInt(1);
                      }
                  }
              } else {
                  // 파일 업로드가 있는 경우: PL/SQL 블록을 사용하여 thumbnails와 articles 모두 INSERT
                  String query = 
                      "DECLARE " +
                      "  v_t_seq NUMBER; " +
                      "  v_a_seq NUMBER; " +
                      "BEGIN " +
                      "  SELECT thumbnails_seq.NEXTVAL, articles_seq.NEXTVAL INTO v_t_seq, v_a_seq FROM dual; " +
                      "  INSERT INTO thumbnails (idx, ofile, sfile, file_path, file_size, file_type, created_at) " +
                      "  VALUES (v_t_seq, ?, ?, ?, ?, ?, ?); " +
                      "  INSERT INTO articles (idx, title, content, category, created_at, visitcnt, members_id, thumbnails_idx) " +
                      "  VALUES (v_a_seq, ?, ?, ?, ?, 0, ?, v_t_seq); " +
                      "  ? := v_a_seq; " +
                      "END;";
                  CallableStatement cstmt = con.prepareCall(query);
                  // thumbnails INSERT 파라미터
                  cstmt.setString(1, dto.getOfile());
                  cstmt.setString(2, dto.getSfile());
                  cstmt.setString(3, dto.getFile_path());
                  cstmt.setLong(4, dto.getFile_size());
                  cstmt.setString(5, dto.getFile_type());
                  cstmt.setTimestamp(6, dto.getCreated_at());
                  // articles INSERT 파라미터
                  cstmt.setString(7, dto.getTitle());
                  cstmt.setString(8, dto.getContent());
                  cstmt.setInt(9, dto.getCategory());
                  cstmt.setTimestamp(10, dto.getCreated_at());
                  cstmt.setString(11, dto.getMembers_id());
                  cstmt.registerOutParameter(12, java.sql.Types.INTEGER);
                  cstmt.execute();
                  articleId = cstmt.getInt(12);
              }
          } catch (Exception e) {
              System.out.println("기사 입력 중 예외 발생");
              e.printStackTrace();
          }
          return articleId;
    }
    

    // 주어진 일련번호에 해당하는 기사을 DTO에 담아 반환합니다.
    public ArticlesDTO selectView(String idx, String memberId) {
        ArticlesDTO dto = new ArticlesDTO(); // DTO 객체 생성
        String query = ""
                + "SELECT idx, title, content, category, created_at, visitcnt, members_id, thumbnails_idx "
                + "FROM articles "
                + "WHERE idx = ?";
        
        try {
            psmt = con.prepareStatement(query); // 쿼리문 준비
            psmt.setString(1, idx); // 게시글 ID 설정
            rs = psmt.executeQuery(); // 쿼리문 실행

            if (rs.next()) { // 결과를 DTO 객체에 저장
                dto.setIdx(rs.getInt(1));
                dto.setTitle(rs.getString(2));
                dto.setContent(rs.getString(3));
                dto.setCategory(rs.getInt(4));
                dto.setCreated_at(rs.getTimestamp(5));
                dto.setVisitcnt(rs.getInt(6));
                dto.setMembers_id(rs.getString(7));
                dto.setThumbnails_idx(rs.getInt(8));
                
                // 해시태그
                dto.setHashtags(hashtagsByArticle(rs.getInt(1)));
                
                // 썸네일 정보 로드
	            loadThumbnail(dto);
                
                // 좋아요 수와 현재 사용자의 좋아요 여부
	            dto.setLikes(getLikeCount(rs.getInt(1)));
	            dto.setIs_liked(isLiked(memberId, rs.getInt(1)));
                
            }
        } catch (Exception e) {
            System.out.println("기사 상세보기 중 예외 발생");
            e.printStackTrace();
        }
        return dto; // 결과 반환
    }

    // 주어진 일련번호에 해당하는 기사의 조회수를 1 증가시킵니다.
    public void updateVisitCount(String idx) {
        String query = "UPDATE articles SET "
                + " visitcnt=visitcnt+1 "
                + " WHERE idx=?";
        try {
            psmt = con.prepareStatement(query);
            psmt.setString(1, idx);
            psmt.executeQuery();
        } catch (Exception e) {
            System.out.println("기사 조회수 증가 중 예외 발생");
            e.printStackTrace();
        }
    }

    public int deletePost(String articleIdx) {
        int result = 0;
        Integer thumbnailIdx = null;
        
        try {
            // 0. 게시글에 연결된 썸네일 인덱스 조회
            String selectThumbQuery = "SELECT thumbnails_idx FROM articles WHERE idx = ?";
            psmt = con.prepareStatement(selectThumbQuery);
            psmt.setString(1, articleIdx);
            ResultSet rs = psmt.executeQuery();
            if (rs.next()) {
                thumbnailIdx = rs.getInt("thumbnails_idx");
            }
            rs.close();
            psmt.close();
    
            // 1. 해시태그 연관 레코드 삭제
            String deleteHashtagsArticlesQuery = "DELETE FROM hashtags_articles WHERE articles_idx = ?";
            psmt = con.prepareStatement(deleteHashtagsArticlesQuery);
            psmt.setString(1, articleIdx);
            psmt.executeUpdate();
            psmt.close();
    
            // 2. 좋아요 연관 레코드 삭제
            String deleteLikesQuery = "DELETE FROM likes WHERE articles_idx = ?";
            psmt = con.prepareStatement(deleteLikesQuery);
            psmt.setString(1, articleIdx);
            psmt.executeUpdate();
            psmt.close();
    
            // 3. 코멘트 연관 레코드 삭제
            String deleteCommentsQuery = "DELETE FROM comments WHERE articles_idx = ?";
            psmt = con.prepareStatement(deleteCommentsQuery);
            psmt.setString(1, articleIdx);
            psmt.executeUpdate();
            psmt.close();
    
            // 4. 게시글 자체 삭제
            String deleteArticleQuery = "DELETE FROM articles WHERE idx = ?";
            psmt = con.prepareStatement(deleteArticleQuery);
            psmt.setString(1, articleIdx);
            result = psmt.executeUpdate();
            psmt.close();
    
            // 5. 연결된 썸네일 삭제 (조회한 thumbnails_idx를 사용)
            if (thumbnailIdx != null) {
                String deleteThumbnailQuery = "DELETE FROM thumbnails WHERE idx = ?";
                psmt = con.prepareStatement(deleteThumbnailQuery);
                psmt.setInt(1, thumbnailIdx);
                psmt.executeUpdate();
                psmt.close();
            }
        } catch (Exception e) {
            System.out.println("기사 삭제 중 예외 발생");
            e.printStackTrace();
        }
        return result;
    }


    // 게시글 데이터를 받아 DB에 저장되어 있던 내용을 갱신합니다(파일 업로드 지원).
    public int updatePost(ArticlesDTO dto, String idx, String thumb_idx) {
        int updatedArticleId = 0;
        try {
            // 이미지 업로드 없이 단순 업데이트인 경우
            if (dto.getOfile() == null || dto.getOfile().trim().equals("")) {
                String query = "UPDATE articles " +
                               "SET title = ?, content = ?, category = ?, created_at = ? " +
                               "WHERE idx = ?";
                psmt = con.prepareStatement(query);
                psmt.setString(1, dto.getTitle());
                psmt.setString(2, dto.getContent());
                psmt.setInt(3, dto.getCategory());
                psmt.setTimestamp(4, dto.getCreated_at());
                psmt.setString(5, idx);
                int result = psmt.executeUpdate();
                if (result > 0) {
                    updatedArticleId = Integer.parseInt(idx);
                }
            } else {
                // 이미지 업로드가 있는 경우
                // thumb_idx 값에 따라 기존 썸네일이 있는지 판단
                if (thumb_idx != null && thumb_idx.trim().equals("0")) {
                    // 기존 썸네일은 없는데 새로 썸네일은 넣은 경우
                    String insertThumbQuery = 
                        "INSERT INTO thumbnails (idx, ofile, sfile, file_path, file_size, file_type, created_at) " +
                        "VALUES (thumbnails_seq.nextval, ?, ?, ?, ?, ?, ?)";
                    PreparedStatement pstmtThumb = con.prepareStatement(insertThumbQuery, new String[]{"idx"});
                    pstmtThumb.setString(1, dto.getOfile());
                    pstmtThumb.setString(2, dto.getSfile());
                    pstmtThumb.setString(3, dto.getFile_path());
                    pstmtThumb.setLong(4, dto.getFile_size());
                    pstmtThumb.setString(5, dto.getFile_type());
                    pstmtThumb.setTimestamp(6, dto.getCreated_at());
                    int thumbResult = pstmtThumb.executeUpdate();
                    int newThumbId = 0;
                    if (thumbResult > 0) {
                        ResultSet rs = pstmtThumb.getGeneratedKeys();
                        if (rs.next()) {
                            newThumbId = rs.getInt(1);
                        }
                        rs.close();
                    }
                    pstmtThumb.close();
                    
                    // articles 테이블 업데이트 (새 썸네일 키 반영)
                    String updateArticleQuery = 
                        "UPDATE articles " +
                        "SET title = ?, content = ?, category = ?, created_at = ?, thumbnails_idx = ? " +
                        "WHERE idx = ?";
                    PreparedStatement pstmtArticle = con.prepareStatement(updateArticleQuery);
                    pstmtArticle.setString(1, dto.getTitle());
                    pstmtArticle.setString(2, dto.getContent());
                    pstmtArticle.setInt(3, dto.getCategory());
                    pstmtArticle.setTimestamp(4, dto.getCreated_at());
                    pstmtArticle.setInt(5, newThumbId);
                    pstmtArticle.setString(6, idx);
                    int artResult = pstmtArticle.executeUpdate();
                    if (artResult > 0) {
                        updatedArticleId = Integer.parseInt(idx);
                    }
                    pstmtArticle.close();
                } else {
                   // 기존 썸네일이 있는데 썸네일을 바꾼 경우
                   String query = 
                    "BEGIN " +
                    "UPDATE thumbnails SET ofile = ?, sfile = ?, file_path = ?, file_size = ?, file_type = ?, created_at = ? WHERE idx = ?; " +
                    "UPDATE articles SET title = ?, content = ?, category = ?, created_at = ?, thumbnails_idx = ? WHERE idx = ?; " +
                    "END;";
                    CallableStatement cstmt = con.prepareCall(query);
                    
                    // thumbnails 업데이트 파라미터 (순서대로)
                    cstmt.setString(1, dto.getOfile());
                    cstmt.setString(2, dto.getSfile());
                    cstmt.setString(3, dto.getFile_path());
                    cstmt.setLong(4, dto.getFile_size());
                    cstmt.setString(5, dto.getFile_type());
                    cstmt.setTimestamp(6, dto.getCreated_at());
                    cstmt.setString(7, thumb_idx);
                    // articles 업데이트 파라미터 (순서대로)
                    cstmt.setString(8, dto.getTitle());
                    cstmt.setString(9, dto.getContent());
                    cstmt.setInt(10, dto.getCategory());
                    cstmt.setTimestamp(11, dto.getCreated_at());
                    cstmt.setString(12, thumb_idx);
                    cstmt.setString(13, idx);
                    
                    cstmt.execute();
                    updatedArticleId = Integer.parseInt(idx);
                    cstmt.close();
                }
            }
        } catch (Exception e) {
            System.out.println("기사 수정 중 예외 발생");
            e.printStackTrace();
        }
        return updatedArticleId;
    }
    
    // 해시태그 처리: 해시태그 문자열을 받아 파싱 후 해시태그 테이블과 교차테이블에 삽입
    public void processHashtags(int articleId, String hashtagStr) {
        if (hashtagStr == null || hashtagStr.trim().isEmpty()) return;
        String[] tags = hashtagStr.split("\\s");
        for (String tag : tags) {
            tag = tag.trim();
            if (tag.startsWith("#")) {
                tag = tag.substring(1);
            }
            if (tag.isEmpty()) continue;
            int hashtagId = getHashtagId(tag);
            if (hashtagId == 0) {
                hashtagId = insertHashtag(tag);
            }
            insertHashtagArticleRelation(hashtagId, articleId);
        }
    }
    
    // 해시태그 존재 확인. 존재하면 해당 idx 반환, 없으면 0 반환
    private int getHashtagId(String tag) {
        int id = 0;
        String sql = "SELECT idx FROM hashtags WHERE name = ?";
        try {
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, tag);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("idx");
            }
            rs.close();
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
    
    // 새로운 해시태그 삽입 후 생성된 idx 반환
    private int insertHashtag(String tag) {
        int id = 0;
        String sql = "INSERT INTO hashtags(idx, name) VALUES(hashtags_seq.nextval, ?)";
        try {
            PreparedStatement pstmt = con.prepareStatement(sql, new String[] { "idx" });
            pstmt.setString(1, tag);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getInt(1);
                }
                rs.close();
            }
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
    
    // 해시태그와 게시글 관계를 교차 테이블에 삽입
    private void insertHashtagArticleRelation(int hashtagId, int articleId) {
        String sql = "INSERT INTO hashtags_articles(hashtags_idx, articles_idx) VALUES(?, ?)";
        try {
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, hashtagId);
            pstmt.setInt(2, articleId);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    //현재 기사 해시태그 목록 조회
    private Set<String> getCurrentHashtags(int articleId) {
        Set<String> currentTags = new HashSet<>();
        String sql = "SELECT h.name FROM hashtags h " +
                     "JOIN hashtags_articles hr ON h.idx = hr.hashtags_idx " +
                     "WHERE hr.articles_idx = ?";
        try {
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, articleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                currentTags.add(rs.getString("name"));
            }
            rs.close();
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentTags;
    }
    

 // 게시글의 기존 해시태그 관계 삭제
    private void deleteHashtagArticleRelation(int hashtagId, int articleId) {
        String sql = "DELETE FROM hashtags_articles WHERE hashtags_idx = ? AND articles_idx = ?";
        try {
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, hashtagId);
            pstmt.setInt(2, articleId);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    //업데이트할 해시태그 목록 파싱
    private Set<String> parseHashtags(String hashtagStr) {
        Set<String> tags = new HashSet<>();
        if (hashtagStr == null || hashtagStr.trim().isEmpty()) return tags;
        
        String[] tokens = hashtagStr.split("\\s");
        for (String tag : tokens) {
            tag = tag.trim();
            if (tag.startsWith("#")) {
                tag = tag.substring(1);
            }
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    //기사 수정시 해시태그 업데이트
    public void updateArticleHashtagsSelective(int articleId, String hashtagStr) {
        Set<String> newTags = parseHashtags(hashtagStr);
        Set<String> currentTags = getCurrentHashtags(articleId);
        
        // 삭제할 해시태그: 기존에는 있지만 새 목록에는 없는 해시태그
        Set<String> tagsToDelete = new HashSet<>(currentTags);
        tagsToDelete.removeAll(newTags);
        
        // 추가할 해시태그: 새 목록에 있지만 기존에는 없는 해시태그
        Set<String> tagsToAdd = new HashSet<>(newTags);
        tagsToAdd.removeAll(currentTags);
        
        // 삭제 처리
        for (String tag : tagsToDelete) {
            int hashtagId = getHashtagId(tag);
            if (hashtagId != 0) {
            	deleteHashtagArticleRelation(hashtagId, articleId);
            }
        }
        
        // 추가 처리
        for (String tag : tagsToAdd) {
            int hashtagId = getHashtagId(tag);
            if (hashtagId == 0) {
                hashtagId = insertHashtag(tag);
            }
            insertHashtagArticleRelation(hashtagId, articleId);
        }
    }
    
    // 썸네일 정보
    public void loadThumbnail(ArticlesDTO dto) {
        if (dto.getThumbnails_idx() != null) {
            String thumbnailQuery = "SELECT ofile, sfile, file_path, file_size, file_type FROM thumbnails WHERE idx = ?";
            try (PreparedStatement thumbnailPsmt = con.prepareStatement(thumbnailQuery)) {
                thumbnailPsmt.setInt(1, dto.getThumbnails_idx());
                ResultSet thumbnailRs = thumbnailPsmt.executeQuery();
                if (thumbnailRs.next()) {
                    dto.setOfile(thumbnailRs.getString("ofile"));
                    dto.setSfile(thumbnailRs.getString("sfile"));
                    dto.setFile_path(thumbnailRs.getString("file_path"));
                    dto.setFile_size(thumbnailRs.getLong("file_size"));
                    dto.setFile_type(thumbnailRs.getString("file_type"));
                }
            } catch (Exception e) {
                System.out.println("썸네일 조회 중 예외 발생");
                e.printStackTrace();
            }
        }
    }
    
	// 상위 10개 기사 반환(top10)
	public List<ArticlesDTO> selectTopArticles() {
		List<ArticlesDTO> article=new ArrayList<ArticlesDTO>();
		String query=""
				+"select * "
				+"  from (select * "
				+"		  from articles a join (select articles_idx, count(members_id) likecnt "
				+"								from likes "
				+"								group by articles_idx) l "
				+"		  on a.idx=l.articles_idx "
				+"		  order by likecnt desc, visitcnt) "
				+" where rownum <= 10";
		
		try {
			psmt=con.prepareStatement(query); // 동적쿼리
			rs=psmt.executeQuery();
			while(rs.next()) {
				ArticlesDTO dto=new ArticlesDTO();
				dto.setIdx(rs.getInt(1));
                dto.setTitle(rs.getString(2));
                dto.setContent(rs.getString(3));
                dto.setCategory(rs.getInt(4));
                dto.setCreated_at(rs.getTimestamp(5));
                dto.setVisitcnt(rs.getInt(6));
                dto.setMembers_id(rs.getString(7));
                dto.setThumbnails_idx(rs.getInt(8));
                dto.setLikes(rs.getInt(10));
                dto.setHashtags(hashtagsByArticle(rs.getInt(1)));  // 해시태그 목록 추가
                loadThumbnail(dto);
				article.add(dto);
			}
		}catch(Exception e) {
			System.out.println("기사 조회 중 예외 발생");
			e.printStackTrace();
		}
		return article;
	}
	
	// 기사 번호로 해당되는 해시태그들 반환
	public List<String> hashtagsByArticle(int articleIdx) {
		List<String> hashtags=new ArrayList<String>();
		String hashtagQuery = "SELECT h.name " +
							  "  FROM hashtags_articles ha JOIN hashtags h " +
							  "  ON ha.hashtags_idx = h.idx " +
							  " WHERE ha.articles_idx = ?";
	    try {
	    	PreparedStatement tagPstmt = con.prepareStatement(hashtagQuery);
            tagPstmt.setInt(1, articleIdx);
            ResultSet tagRs = tagPstmt.executeQuery();
	        while(tagRs.next()) {
                hashtags.add(tagRs.getString("name"));
	        }
	        tagRs.close();
	        tagPstmt.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return hashtags;
	}
	
	// 좋아요 갯수
	public int getLikeCount(int idx) {
	    String query = "SELECT COUNT(*) FROM likes WHERE articles_idx = ?";
	    try {
	    	psmt=con.prepareStatement(query);
	        psmt.setInt(1, idx);
	        ResultSet rs = psmt.executeQuery();
	        if (rs.next()) {
	            return rs.getInt(1);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return 0; // 기본값: 좋아요가 없음
	}
	// 좋아요 추가
	public boolean addLike(String members_id, int articleIdx) {
	    String query = "INSERT INTO likes (members_id, articles_idx) VALUES (?, ?)";
	    try {
	    	psmt=con.prepareStatement(query);
	        psmt.setString(1, members_id);
	        psmt.setInt(2, articleIdx);
	        return psmt.executeUpdate() > 0;
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	// 좋아요 제거
	public boolean removeLike(String members_id, int articleIdx) {
	    String query = "DELETE FROM likes WHERE members_id = ? AND articles_idx = ?";
	    try {
	    	psmt=con.prepareStatement(query);
	        psmt.setString(1, members_id);
	        psmt.setInt(2, articleIdx);
	        return psmt.executeUpdate() > 0;
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	// 좋아요 여부
	public boolean isLiked(String members_id, int idx) {
	    String query = "SELECT COUNT(*) FROM likes WHERE members_id = ? AND articles_idx = ?";
	    try {
	    	psmt=con.prepareStatement(query);
	        psmt.setString(1, members_id);
	        psmt.setInt(2, idx);
	        ResultSet rs = psmt.executeQuery();
	        if (rs.next()) {
	            return rs.getInt(1) > 0; // 1보다 크면 좋아요가 있음
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // 기본값: 좋아요가 없음
	}

}