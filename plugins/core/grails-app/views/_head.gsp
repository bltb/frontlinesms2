<%@ page import="frontlinesms2.Fconnection" %>
<div id="head">
	<div id="main-nav">
		<ul>
			<fsms:tab controller="message" mainNavSection="${mainNavSection}">
				<span id="inbox-indicator" class="">${frontlinesms2.Fmessage.countUnreadMessages()}</span>
			</fsms:tab>
			<fsms:tab controller="archive" mainNavSection="${mainNavSection}"/>
			<fsms:tab controller="contact" mainNavSection="${mainNavSection}"/>
			<fsms:tab controller="status" mainNavSection="${mainNavSection}">
				<fsms:trafficLightStatus/>
			</fsms:tab>
			<fsms:tab controller="search" mainNavSection="${mainNavSection}"/>
		</ul>
	</div>
</div>

