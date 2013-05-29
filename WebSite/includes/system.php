<?

include("includes/config.php");
include("includes/progconfig.php");
include("includes/hypersignal.class.php");
include("includes/facebook.php");


//	HyperSignal
$hsman	=	new HyperSignal($host,$user,$pass,$database,$progver,$replacelist);
if($loadfb)	{
	//	Facebook
	$facebook = new Facebook(array(
	  'appId'  => $fbappid,
	  'secret' => $fbsecret,
	));
	if($_REQUEST["c"] == "logout")	{
		session_destroy();
		$facebook->destroySession();
	    header("Location: $siteurl/fb.php");
	}
	$user = $facebook->getUser();
	if ($user) {
	  try {
	    // Proceed knowing you have a logged in user who's authenticated.
	    $user_profile = $facebook->api('/me');
	  } catch (FacebookApiException $e) {
	    error_log($e);
	    $user = null;
	  }
	}

	if ($user) {
	  $logoutUrl = $facebook->getLogoutUrl($params = array('next' => $siteurl."fb.php?c=logout"));
	} else {
	  $loginUrl = $facebook->getLoginUrl($params = array('redirect_uri' => $siteurl."fb.php?frame=1", 'req_perms' => 'publish_stream', 'scope' => 'email'));
	}

	if($user) {
		if(!$hsman->checkUser($user_profile["id"])) {
			$hsman->addUser($user_profile["id"], $user_profile["username"], $user_profile["name"], $user_profile["email"], $_SERVER["REMOTE_ADDR"], $user_profile["location"]["name"]);
		}else{
			$hsman->updateUserEnter($user_profile["id"], $_SERVER["REMOTE_ADDR"]);
		}
		$loginSection = 'Bem-vindo '.$user_profile["name"].'! <BR><center><a href="'.$logoutUrl.'">Sair</a></center>';
	}else{
		$loginSection = '<a href="'.$loginUrl.'" target="_parent"><img border=0 src="'.$siteurl.'images/fblogin.png"/></a>';
	}

}else{
//	Página

$bodyonload = "";
$head = "";
$page = "";

$pagerequest = $_REQUEST["page"];
$uri	=	str_ireplace(str_ireplace("http://".$_SERVER["SERVER_NAME"]."","",$siteurl), "", $_SERVER["REQUEST_URI"]);
$uri = explode("/",$uri);

switch($uri[0]) {
	case "page":
		switch(strtolower($pagerequest)) {
			case "sobre":
				$page = HyperSignal::loadText("bases/sobre.html");
				
				break;
			case "aparelhos":
				$page = "<div align=\"center\"><h1>Aparelhos testados</h1> <BR>";
				$devices = $hsman->getDevices();
				foreach($devices as $device) {
					$page .= "<B>Dispositivo:</B> ".$device["device"]." <B>Fabricante:</B> ".$device["manufacturer"]." <B>Modelo:</B> ".$device["model"]." <B>Marca:</B> ".$device["brand"]." <B>Android:</B> ".$device["android"]."-".$device["release"]."<BR>";
				}
				$page .= "</div>";
				break;
			default: 
				$page = HyperSignal::loadText("bases/home.html");
				break;
		}
	break;
	case "maps":
		$page = HyperSignal::loadText("bases/mapframe.html");
		$bodyonload = "initialize();";
		$head = "	<link href=\"https://code.google.com/apis/maps/documentation/javascript/examples/default.css\" rel=\"stylesheet\" type=\"text/css\" />
					<script type=\"text/javascript\" src=\"https://maps.googleapis.com/maps/api/js?sensor=false\"></script>
					<script type=\"text/javascript\" src=\"$apiurl/?jscript=json\"></script>
					<script type=\"text/javascript\" src=\"$apiurl/?jscript=tileoverlay\"></script>
					<script type=\"text/javascript\" src=\"$apiurl/?jscript=stracker\"></script>";
		$page = str_ireplace("{TOPSEC}","",$page);
		if(!empty($uri[3])) {
			$preaddress = "<script type=\"text/javascript\"> var preaddress = \"".addslashes(urldecode($uri[3]))."\"; var preoperator = \"".strtoupper(addslashes(urldecode($uri[4])))."\"; </script>\n";
		}else{
			$preaddress = "<script type=\"text/javascript\"> var preaddress = \"\"; var preoperator = \"\"; </script>\n";
		}
		break;
	default: 
		$page = HyperSignal::loadText("bases/home.html");
		break;
}

$topusers		= 	$hsman->getTopList();
$topcounter		= 	1;
$page			.=	"<div id=\"toplist\" class=\"toplist\"><center><font style=\"font-size:15px\">Maiores Contribuidores</font><BR><font style=\"font-size:10px;\">";
foreach($topusers as $topuser) {
	$page .= $topcounter."º - ".$topuser["name"]." - ".$topuser["sentkm"]." km<BR>";
	$topcounter++;
} 
$page			.=	"</font></center></div>";

$page			=	"<BR>".$page;
$page			=	$hsman->ReplaceList($page);	

$page			=	str_ireplace("{PROG_VER}",$progver,$page);
}
?>
