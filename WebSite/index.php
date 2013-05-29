<?
$loadfb = false;
include("includes/system.php");
?>
<html>
<head>
	<title>Signal Tracker</title>
	
	<meta name="title" content="Signal Tracker">
	<meta name="url" content="<? echo $siteurl; ?>">
	<meta name="description" content="Veja o mapa de cobertura de sua operadora e contribua!">
	<meta name="keywords" content="SignalTracker, Signal, Tracker, Signal Tracker, VIVO, CLARO, OI, TIM, ANATEL, Operadoras, Celulares, Sinal, Cobertura">
	<meta http-equiv="Content-Type" content="text/html;charset=iso-8859-1"><meta name="autor" content="Lucas Teske">
	<meta name="company" content="Signal Tracker">
	<meta name="revisit-after" content="1">
	<meta property="og:title" content="Signal Tracker"/> 
	<meta property="og:type" content="other:technology"/> 
	<meta property="og:url" content="<? echo $siteurl; ?>"/> 
	<meta property="og:site_name" content="Signal Tracker"/> 
	<meta property="og:image" content="<? echo $siteurl; ?>images/tower.png"/> 
	<meta property="og:description" content="Veja o mapa de cobertura de sua operadora e contribua!"/>
	<meta property="fb:admins" content="1748067536"/>
	
	<meta http-equiv="X-UA-Compatible" content="IE=9" />
	<link rel="stylesheet" href="<? echo $apiurl; ?>?css=menu_style" type="text/css" />
	<link rel="stylesheet" href="<? echo $apiurl; ?>?css=signaltracker" type="text/css" />
	<link rel="shortcut icon" href="<? echo $siteurl; ?>favicon.ico" type="image/x-icon"/>
	<script type="text/javascript" src="<? echo $apiurl; ?>?jscript=/jquery"></script>

	<?php echo $preaddress; echo $head; ?>
</head>
<body onLoad="<?php echo $bodyonload; ?>">
<div id="logo" class="logo"><center><img src="<? echo $siteurl; ?>images/logo.png"></center></div>
<div id="login" class="login">
	<center>
	<iframe class="fbframe" src="<? echo $siteurl;?>fb.php"></iframe>
	</center>
</div>
<div id="menu_box" class="menu_box">
	<div class="menu">
		<ul>
			<li><a href="<? echo $siteurl; ?>" >Home</a></li>
			<li><a href="javascript:void(0);" id="current">Ver Cobertura</a>
				<ul>
					<li><a href="<? echo $siteurl; ?>maps/">Todos (<font color=green>Tile Maps</font>)</a></li>
			   </ul>
		    </li>
			</li>
			<li><a href="javascript:void(0);">Sobre</a>
                <ul>
				<li><a href="<? echo $siteurl; ?>page/aparelhos">Lista de aparelhos testados</a></li>
				<!-- <li><a href="/OpSignal/page/statistics">Estatísticas de Uso</a></li> -->
                <li><a href="<? echo $siteurl; ?>page/sobre">Sobre o projeto</a></li>
                </ul>
          </li>
		</ul>
	</div>
</div>
<div id="androidmarket" class="androidmarket"><a href="https://market.android.com/details?id=com.mobilles.signalsampler"><img border=0 src="<? echo $siteurl; ?>images/qrcode.png"></a></div>
<div id="content">
<?php echo $page; ?>
</div>
</body>
