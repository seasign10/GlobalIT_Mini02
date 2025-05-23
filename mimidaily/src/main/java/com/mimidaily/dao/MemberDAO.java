package com.mimidaily.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mimidaily.common.DBConnPool;
import com.mimidaily.dto.MemberDTO;
import com.mimidaily.dto.MemberInfoDTO;

//public class MemberDAO {
public class MemberDAO extends DBConnPool {
	public MemberDAO() {
		super();
	}
	
	// 로그인시 사용하는 메서드
	public int userCheck(String userid, String pwd) {
		int result = -1; // 기본값
		String query = "select pwd from members where id = ?"; // 쿼리문 템플릿 준비
		try {
			psmt = con.prepareStatement(query); // 쿼리문 준비
			psmt.setString(1, userid); // 인파라미터 설정
			rs = psmt.executeQuery(); // 쿼리문 실행

			if (rs.next()) {
				if (rs.getString("pwd") != null &&
						rs.getString("pwd").equals(pwd)) {
					result = 1; // 비밀번호일치
				} else {
					result = 0; // 비밀번호 불일치
				}
			} else {
				result = -1; // i id 없음
			}
		} catch (Exception e) {
			System.out.println("게시물 상세보기 중 예외 발생");
			e.printStackTrace();
		}
		return result;
	}
	
	// 비밀번호 체크 메서드
	public int pwdChk(String pwd, String memberId) {
		int result=-1;
		String query="select * from members where pwd=? and id=?";
		try {
			psmt=con.prepareStatement(query);
			psmt.setString(1, pwd);
			psmt.setString(2, memberId);
			rs=psmt.executeQuery();
			if (rs.next()) {
	            result = 1; // 조회 성공
	        }
		}catch(Exception e) {
			System.out.print("비밀번호 오류");
			e.printStackTrace();
		}
		return result;
	}
	
	// 역할을 가져오는 메서드
    public int getUserRole(String userid) {
        int role = 0;
        String query = "SELECT role FROM members WHERE id = ?";
        try {
            psmt = con.prepareStatement(query);
            psmt.setString(1, userid);
            rs = psmt.executeQuery();

            if (rs.next()) {
                role = rs.getInt("role"); // 역할 가져오기
            }
        } catch (Exception e) {
            System.out.println("역할 조회 중 예외 발생");
            e.printStackTrace();
        }
        return role; // 역할 반환
    }
    
    // 방문횟수 조회 및 증가
    public int incrementUserVisitCnt(String userid, boolean isVisited) {
        int visitcnt = 0;
        String selectSql = "SELECT visitcnt FROM members WHERE id = ?"; // 방문 횟수 조회 SQL
        String updateSql = "UPDATE members SET visitcnt = ? WHERE id = ?"; // 방문 횟수 증가 SQL

        try {
            // 방문 횟수 조회
            psmt = con.prepareStatement(selectSql);
            psmt.setString(1, userid);
            rs = psmt.executeQuery();

            if (rs.next()) {
                visitcnt = rs.getInt("visitcnt"); // 현재 방문 횟수 가져오기
            }
            
            
            if(!isVisited) {
            	// 방문 횟수 증가
            	visitcnt++; // 1 증가            	
            }

            // 업데이트 실행
            psmt = con.prepareStatement(updateSql);
            psmt.setInt(1, visitcnt);
            psmt.setString(2, userid);
            psmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } 
        return visitcnt; // 증가된 방문 횟수 반환
    }
    
    // 멤버정보 가져오기(필요한 정보만)
    public MemberInfoDTO getMemberInfo(String memberId) {
        MemberInfoDTO memberInfo = null;
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM articles WHERE members_id = ?) AS article_count, "
                + "(SELECT COUNT(*) FROM comments WHERE members_id = ?) AS comment_count, "
                + "id, name, created_at "
                + "FROM members "
                + "WHERE id = ?";

        try {
        	psmt = con.prepareStatement(sql);
            psmt.setString(1, memberId);
            psmt.setString(2, memberId);
            psmt.setString(3, memberId);
            rs = psmt.executeQuery();

            if (rs.next()) {
                memberInfo = new MemberInfoDTO();
                memberInfo.setId(rs.getString("id"));
                memberInfo.setName(rs.getString("name"));
                memberInfo.setArticleCount(rs.getInt("article_count"));
                memberInfo.setCommentCount(rs.getInt("comment_count"));
                memberInfo.setCreatedAt(rs.getTimestamp("created_at"));
                MemberDTO mDto = getMember(memberInfo.getId());
				if (mDto != null) {
					memberInfo.setProfiles(mDto);
				}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return memberInfo;
    }
  
	//id 중복 확인
	public int confirmID(String userid) {
		int result = -1; // 기본값
		String sql = "select id from members where id=?";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, userid);
			rs = psmt.executeQuery();
			if (rs.next()) {
				result = 1;// 사용 불가능
			} else {
				result = -1;// 사용 가능
			}
		} catch (Exception e) {
			System.out.println("id 중복 확인 중 오류 발생");
			e.printStackTrace();
		}
		return result;
	}
	
	// 회원 등록
	public int insertMember(MemberDTO mDto) {
		int result = -1;
		String sql = "insert into members(id, pwd, name, email, tel, birth, gender, role, marketing, created_at) VALUES \r\n"
				+ "    (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, mDto.getId());
			psmt.setString(2, mDto.getPwd());
			psmt.setString(3, mDto.getName());
			psmt.setString(4, mDto.getEmail());
			psmt.setString(5, mDto.getTel());
			psmt.setString(6, mDto.getBirth());
			psmt.setString(7, mDto.getGender());
			psmt.setInt(8, mDto.getRole());
			psmt.setBoolean(9, mDto.isMarketing());
			result = psmt.executeUpdate();// 영향을 받은 행의 수 리턴. insert하면 1행이 추가되므로 1이 리턴됨.
		} catch (Exception e) {
			System.out.println("회원 등록 중 오류 발생");
			e.printStackTrace();
		}
		return result;
	}
		
	// 회원 정보 검색
	public MemberDTO getMember(String id) {
		MemberDTO mDto = null;
		String sql = "select * from members where id=?";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, id);
			rs = psmt.executeQuery();
			if (rs.next()) {
				//Model 객체에 입력한 값을 저장
				mDto = new MemberDTO();
				mDto.setId(rs.getString("id"));
				mDto.setPwd(rs.getString("pwd"));
				mDto.setName(rs.getString("name"));
				mDto.setEmail(rs.getString("email"));
				mDto.setTel(rs.getString("tel"));
				mDto.setBirth(rs.getString("birth"));
				mDto.setGender(rs.getString("gender"));
				mDto.setMarketing(rs.getBoolean("Marketing"));
				mDto.setProfile_idx(rs.getInt("profiles_idx"));
				loadProfile(mDto);
			}
		} catch (Exception e) {
			System.out.println("회원 정보 검색 중 오류 발생");
			e.printStackTrace();
		}
		return mDto;
	}
	
	// 프로필 정보
    public void loadProfile(MemberDTO dto) {
        if (dto.getProfile_idx() != null) {
            String profileQuery = "SELECT ofile, sfile, file_path, file_size, file_type FROM profiles WHERE idx = ?";
            try (PreparedStatement profilePsmt = con.prepareStatement(profileQuery)) {
                profilePsmt.setInt(1, dto.getProfile_idx());
                ResultSet profileRs = profilePsmt.executeQuery();
                if (profileRs.next()) {
                    dto.setOfile(profileRs.getString("ofile"));
                    dto.setSfile(profileRs.getString("sfile"));
                    dto.setFile_path(profileRs.getString("file_path"));
                    dto.setFile_size(profileRs.getLong("file_size"));
                    dto.setFile_type(profileRs.getString("file_type"));
                }
            } catch (Exception e) {
                System.out.println("프로필 조회 중 예외 발생");
                e.printStackTrace();
            }
        }
    }
		
	//회원 정보 수정
	public int updateMember(MemberDTO mDto) {
		int result = -1;
		String sql = "update members set pwd=?, name=?, email=?, "
				+ " tel=?, birth=?, gender=?, marketing=? where id=?";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, mDto.getPwd());
			psmt.setString(2, mDto.getName());
			psmt.setString(3, mDto.getEmail());
			psmt.setString(4, mDto.getTel());
			psmt.setString(5, mDto.getBirth());
			psmt.setString(6, mDto.getGender());
			psmt.setBoolean(7, mDto.isMarketing());
			psmt.setString(8, mDto.getId());
			result = psmt.executeUpdate();
		} catch (Exception e) {
			System.out.println("회원 정보 수정 중 오류 발생");
			e.printStackTrace();
		} finally {
			close(); }
		return result;
	}
		
	// 회원 정보 데이터를 받아 DB에 저장되어 있던 내용을 갱신(파일 업로드 지원).
	public String updateMember(MemberDTO dto, String profile_idx) {
	    String updated = null;
	    try {
	        // 파일 업로드 없이 단순 업데이트인 경우 (프로필 변경 없음)
	        if (dto.getOfile() == null || dto.getOfile().trim().equals("")) {
	            String query = "UPDATE members " +
	                           "SET pwd=?, name=?, email=?, tel=?, birth=?, gender=?, marketing=? " +
	                           "WHERE id = ?";
	            psmt = con.prepareStatement(query);
	            psmt.setString(1, dto.getPwd());
	            psmt.setString(2, dto.getName());
	            psmt.setString(3, dto.getEmail());
	            psmt.setString(4, dto.getTel());
	            psmt.setString(5, dto.getBirth());
	            psmt.setString(6, dto.getGender());
	            psmt.setBoolean(7, dto.isMarketing());
	            psmt.setString(8, dto.getId());
	            int result = psmt.executeUpdate();
	            if (result > 0) {
	                updated = dto.getId();
	            }
	        } else {
	            // 파일 업로드가 있는 경우
	            // profile_idx 값에 따라 기존 썸네일 레코드가 있는지 판단
	            if (profile_idx == null || profile_idx.trim().isEmpty() || profile_idx.trim().equals("0")) {
	                // profile_idx가 "0"이거나 null, 빈 값이면 새 프로필 생성
	                String insertProfileQuery = 
	                    "INSERT INTO profiles (idx, ofile, sfile, file_path, file_size, file_type, created_at) " +
	                    "VALUES (profiles_seq.nextval, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
	                PreparedStatement psmtProfile = con.prepareStatement(insertProfileQuery, new String[]{"idx"});
	                psmtProfile.setString(1, dto.getOfile());
	                psmtProfile.setString(2, dto.getSfile());
	                psmtProfile.setString(3, dto.getFile_path());
	                psmtProfile.setLong(4, dto.getFile_size());
	                psmtProfile.setString(5, dto.getFile_type());
	                int profileResult = psmtProfile.executeUpdate();
	                int newProfileId = 0;
	                if (profileResult > 0) {
	                    ResultSet rs = psmtProfile.getGeneratedKeys();
	                    if (rs.next()) {
	                        newProfileId = rs.getInt(1);  // 새로 생성된 프로필 ID를 가져옴
	                    }
	                    rs.close();
	                }
	                psmtProfile.close();
	                
	                // newProfileId가 0이면 프로필 삽입이 실패한 것이므로 예외를 발생시킴
	                if (newProfileId == 0) {
	                    throw new SQLException("프로필 생성에 실패했습니다.");
	                }
	                
	                // members 테이블 업데이트 (새 프로필 키 반영)
	                String updateMemberQuery = 
	                    "UPDATE members " +
	                    "SET pwd=?, name=?, email=?, tel=?, birth=?, gender=?, marketing=?, profiles_idx = ? " +
	                    "WHERE id = ?";
	                PreparedStatement psmtMember = con.prepareStatement(updateMemberQuery);
	                psmtMember.setString(1, dto.getPwd());
	                psmtMember.setString(2, dto.getName());
	                psmtMember.setString(3, dto.getEmail());
	                psmtMember.setString(4, dto.getTel());
	                psmtMember.setString(5, dto.getBirth());
	                psmtMember.setString(6, dto.getGender());
	                psmtMember.setBoolean(7, dto.isMarketing());
	                psmtMember.setInt(8, newProfileId);  // 새로 생성된 프로필 ID 삽입
	                psmtMember.setString(9, dto.getId());
	                int memberResult = psmtMember.executeUpdate();
	                if (memberResult > 0) {
	                    updated = dto.getId();
	                }
	                psmtMember.close();
	            } else {
	                // profile_idx가 "0"이 아니면 기존 프로필 레코드가 존재하므로, 
	                // PL/SQL 블록을 사용해 profiles 테이블과 members 테이블 모두 업데이트
	                String query =
	                    "BEGIN " +
	                    "  UPDATE profiles " +
	                    "  SET ofile = ?, " +
	                    "      sfile = ?, " +
	                    "      file_path = ?, " +
	                    "      file_size = ?, " +
	                    "      file_type = ?, " +
	                    "      created_at = SYSTIMESTAMP " +
	                    "  WHERE idx = ?; " +
	                    "  UPDATE members " +
	                    "  SET pwd=?, " +
	                    "      name=?, " +
	                    "      email=?, " +
	                    "      tel=?, " +
	                    "      birth=?, " +
	                    "      gender=?, " +
	                    "      marketing=?, " +
	                    "      profiles_idx = ? " +
	                    "  WHERE id = ?; " +
	                    "END;";

	                CallableStatement cstmt = con.prepareCall(query);

	                // profile_idx 값 검증
	                if (profile_idx == null || profile_idx.trim().isEmpty()) {
	                    throw new SQLException("profile_idx is empty or null");
	                }

	                // profile_idx가 숫자일 경우만 처리
	                int profileId = -1;
	                try {
	                    profileId = Integer.parseInt(profile_idx.trim());  // profile_idx를 숫자로 변환
	                } catch (NumberFormatException e) {
	                    throw new SQLException("Invalid profile_idx format: " + profile_idx, e);
	                }

	                // profiles 업데이트 파라미터 (순서대로)
	                cstmt.setString(1, dto.getOfile());
	                cstmt.setString(2, dto.getSfile());
	                cstmt.setString(3, dto.getFile_path());
	                cstmt.setLong(4, dto.getFile_size());
	                cstmt.setString(5, dto.getFile_type());
	                cstmt.setInt(6, profileId);  // 변환된 profile_id 사용

	                // 회원 정보 업데이트 파라미터 (순서대로)
	                cstmt.setString(7, dto.getPwd());
	                cstmt.setString(8, dto.getName());
	                cstmt.setString(9, dto.getEmail());
	                cstmt.setString(10, dto.getTel());
	                cstmt.setString(11, dto.getBirth());
	                cstmt.setString(12, dto.getGender());
	                cstmt.setInt(13, dto.isMarketing() ? 1 : 0);
	                cstmt.setInt(14, profileId);  // 변환된 profile_id 사용
	                cstmt.setString(15, dto.getId());

	                cstmt.execute();
	                updated = dto.getId();
	                cstmt.close();
	            }
	        }
	    } catch (Exception e) {
	        System.out.println("회원 정보 수정 중 예외 발생");
	        e.printStackTrace();
	    }
	    return updated;
	}
		
	//특정 유저 정보 가져오기(글쓴이/댓글 작성자 정보 가져오기)
	public MemberDTO userInfo(String member_id) {
		MemberDTO mDto = null;
	    String query = "SELECT id, name, email, role, profiles_idx FROM members WHERE id = ?"; // 필요한 컬럼 선택

	    try {
	        psmt = con.prepareStatement(query);
	        psmt.setString(1, member_id); // memberId를 쿼리에 설정
	        rs = psmt.executeQuery();

	        if (rs.next()) {
	            mDto = new MemberDTO(); // MemberDTO 객체 생성
	            mDto.setId(rs.getString("id"));
	            mDto.setName(rs.getString("name"));
	            mDto.setEmail(rs.getString("email"));
	            mDto.setRole(rs.getInt("role"));
	            mDto.setProfile_idx(rs.getInt("profiles_idx"));
				loadProfile(mDto);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		return mDto;
	}
}