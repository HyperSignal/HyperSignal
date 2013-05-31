<?

include("includes/config.php");
include("includes/progconfig.php");
include("includes/hypersignal.class.php");
include("includes/facebook.php");


//	HyperSignal
$hsman	=	new HyperSignal($host,$user,$pass,$database,$progver,$basepath,$lang);
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
		$loginSection = 'Bem-vindo '.$user_profile["name"].'! <BR><center><a href="'.$logoutUrl.'" data-ajax="false">Sair</a></center>';
	}else{
		$loginSection = '<a href="'.$loginUrl.'" target="_parent" data-ajax="false"><img border=0 src="'.$siteurl.'images/fblogin.png"/></a>';
	}

}else{
	//	Página

	$bodyonload = "";
	$head = "";
	$page = "";

	$pagerequest = $_REQUEST["page"];
	$uri	=	str_ireplace(str_ireplace("http://".$_SERVER["SERVER_NAME"]."","",$siteurl), "", $_SERVER["REQUEST_URI"]);
	$uri = explode("/",$uri);
	$preaddress = "var preaddress = \"\"; var preoperator = \"\";\n";
	switch($uri[0]) {
		case "page":
			switch(strtolower($pagerequest)) {
				case "about":
					$page = $hsman->loadTPL("about.html");
					break;
				case "devices":
					$page = "<div align=\"center\"><h1>[TESTEDDEVICES]</h1> <BR>";
					$devices = $hsman->getDevices();

					foreach($devices as $device) {
						$page .= "<B>[DEVICE]:</B> ".$device["device"]." <B>[MANUFACTURER]:</B> ".$device["manufacturer"]." <B>[MODEL]:</B> ".$device["model"]." <B>[BRAND]:</B> ".$device["brand"]." <B>[ANDROID]:</B> ".$device["android"]."-".$device["release"]."<BR>";
					}
					$page .= "</div>";
					break;
				default: 
					$page = $hsman->loadTPL("home.html");
					break;
			}
		break;
		case "maps":
			$page = $hsman->loadTPL("mapframe.html");
			$page = str_ireplace("{TOPSEC}","",$page);
			if(!empty($uri[1])) {
				$preaddress = "var preaddress = \"".addslashes(urldecode($uri[1]))."\"; var preoperator = \"".strtoupper(addslashes(urldecode($uri[2])))."\";\n";
			}else{
				$preaddress = "var preaddress = \"\"; var preoperator = \"\";\n";
			}
			$menu2	=	'<table style=\'width:100%\'><tr><td><a href="javascript:void(0)" onClick="generateLink()"><img src="{SITEURL}images/link.png" width=32 height=32></a></td><td><input type="search" name="search_address" id="search_address" placeholder="[SEARCHADDRESS]" value="" data-mini="true" /></td><td><input type="button" value="[GO]" onClick="procurar()"data-mini="true" data-ajax="false"></td></tr></table>';
			;	
			break;
		default: 
			$page = $hsman->loadTPL("home.html");
			break;
	}

	$replacelist["bodyonload"]	=	$bodyonload;
	$replacelist["preaddress"]	=	$preaddress;
	$replacelist["head"]		=	$head;
	$replacelist["lang"]		=	$lang;
	$replacelist["menu2"]		=	$menu2;

	$topusers		= 	$hsman->getTopList();
	$topcounter		= 	1;
	$page			.=	"		<div id=\"toplist\" class=\"toplist\"><center><font style=\"font-size:15px\">[BESTCONTRIBUTERS]</font><BR><font style=\"font-size:10px;\">";
	foreach($topusers as $topuser) {
		$page .= $topcounter." - ".$topuser["name"]." - ".$topuser["sentkm"]." km<BR>";
		$topcounter++;
	} 
	$page			.=	"</font></center></div>";

	$page			=	"<BR>".$page;
	$fullpage		=	$hsman->loadTPL("mainpage.html");
	$fullpage		=	str_ireplace("{PAGECONTENT}",$page,$fullpage);	
	$fullpage		=	$hsman->ParseLanguage($fullpage);
	$fullpage		=	$hsman->ReplaceList($fullpage,$replacelist);		
	$fullpage		=	$hsman->ParseLanguage($fullpage);
	$fullpage		=	$hsman->ReplaceList($fullpage,$replacelist);

	$fullpage		=	str_ireplace("{PROG_VER}",$progver,$fullpage);
	
}
?>
