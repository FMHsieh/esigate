<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib uri="http://www.esigate.org/taglib" prefix="assemble"%>
<assemble:includeTemplate page="templatemultipleparams.jsp">
	<assemble:includeParam name="title">
		<title>Exemple d'utilisation d'un template (titre ins�r� par
		l'applicatif)</title>
	</assemble:includeParam>
	<assemble:includeParam name="param1">
		<div style="background-color: yellow">Contenu ins�r� par
		l'applicatif<br />
		NB : l'image est servie par la servlet proxy</div>
	</assemble:includeParam>
	<assemble:includeParam name="param2">
		<div style="background-color: yellow">Autre bloc de contenu
		ins�r� par l'applicatif</div>
	</assemble:includeParam>
</assemble:includeTemplate>
