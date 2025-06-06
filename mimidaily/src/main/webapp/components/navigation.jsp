<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<script src="https://kit.fontawesome.com/e7c9242ec2.js" crossorigin="anonymous"></script>
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script type="module" src="/script/nav.js"></script>
<div id="loading-overlay">
	<div class="spinner"></div>
</div>

<nav id="nav"> 
	<div id="logo">
		<h1 class="hide">미미일보 로고</h1>
		<a href="/main.do">
			<img src="${pageContext.request.contextPath}/media/images/logo.svg" alt="미미일보 로고" width="251px" />
		</a>
	</div>
	<ul class="menu">
		<li><a href="${pageContext.request.contextPath}/articles/topten.do">TOP10</a></li>
		<li><a href="${pageContext.request.contextPath}/articles/newest.do">최신기사</a></li>
		<li><a href="${pageContext.request.contextPath}/articles/travel.do">여행지</a></li>
		<li><a href="${pageContext.request.contextPath}/articles/musteat.do">맛집</a></li>
	</ul>
	<div class="menu_icon"><i class="fa-solid fa-bars"></i></div>
	<ul class="nav_toggle">
		<li><a href="${pageContext.request.contextPath}/articles/topten.do">TOP10</a></li>
		<li><a href="${pageContext.request.contextPath}/articles/newest.do">최신기사</a></li>
		<li><a href="${pageContext.request.contextPath}/articles/travel.do">여행지</a></li>
		<li><a href="${pageContext.request.contextPath}/articles/musteat.do">맛집</a></li>
	</ul>
</nav>
<div class="base"></div>