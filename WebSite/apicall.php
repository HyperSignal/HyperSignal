<?php

include "includes/config.php";
include "includes/uploader.class.php";

$fup = new STUploader($host, $user, $pass, $database, $progver);

if(isset($_REQUEST["graph"]))	{
	include  ('jpgraph/jpgraph.php');
	include ('jpgraph/jpgraph_line.php');
	$year	=	(int)($_REQUEST["year"]);
	$month	=	(int)($_REQUEST["month"]);
	$stdata	=	$fup->GetStatisticData($year,$month);
	
	$tts = false;
	$signal = false;
	$apicall = false;
	$tower = false;
	$file = false;
	
	switch($_REQUEST["graph"])	{
		case "all":
			$title	=	"Gráfico de tudo";
			$tts = true;
			$signal = true;
			$apicall = true;
			$tower = true;
			$file = true;
			break;
		case "apicall":
			$title = "Gráfico de Chamadas na API";
			$apicall = true;
			break;
		case "signal":
			$title = "Gráfico de Pontos de Sinal";
			$signal = true;
			break;
		case "tower":
			$title = "Gráfico de Torres Novas";
			$tower = true;
			break;
		case "tts":
			$title = "Gráfico de uso do Teske Tracking System";
			$tts = true;
			break;
		case "file":
			$title = "Gráfico de Arquivos Enviados";
			$file = true;
			break;
		default: 
			$title = "Inválido";
	}
	
	$graph = new Graph(640,480);
	$graph->SetScale("textlin");
	
	$theme_class=new UniversalTheme;
	
	$graph->SetTheme($theme_class);
	
	$graph->title->Set($title);
	$graph->SetBox(false);
		
	$graph->yaxis->HideZeroLabel();
	$graph->yaxis->HideLine(false);
	$graph->yaxis->HideTicks(false,false);
	$graph->ygrid->SetFill(false);
	$graph->SetBackgroundImage("images/graphbg.jpg",BGIMG_FILLFRAME);
	
	$graph->xgrid->Show();
	$graph->xgrid->SetLineStyle("solid");
	$graph->xgrid->SetColor('#A3A3A3');
	
	if($tower)	{
		$p1 = new LinePlot($stdata["tower"]);
		$graph->Add($p1);
		$p1->SetColor($fup->random_color());
		$p1->SetLegend('Torres Novas');
	}
	if($signal)	{
		$p2 = new LinePlot($stdata["signal"]);
		$graph->Add($p2);
		$p2->SetColor($fup->random_color());
		$p2->SetLegend('Pontos novos');
	}
	if($file)	{
		$p3 = new LinePlot($stdata["file"]);
		$graph->Add($p3);
		$p3->SetColor($fup->random_color());
		$p3->SetLegend('Arquivos Enviados');
	}
	if($apicall)	{
		$p4 = new LinePlot($stdata["apicall"]);
		$graph->Add($p4);
		$p4->SetColor($fup->random_color());
		$p4->SetLegend('Chamadas na API');
	}	
	if($tts)	{
		$p5 = new LinePlot($stdata["tts"]);
		$graph->Add($p5);
		$p5->SetColor($fup->random_color());
		$p5->SetLegend('Chamadas do Teske Tracking System');
	}
	$graph->legend->SetFrameWeight(1);
	
	// Output line
	$graph->Stroke();
	
	
}else
	echo '{"result":"NC"}';

?>
