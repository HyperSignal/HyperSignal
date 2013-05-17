<?

$host		= 	"localhost";
$user		=	"root";
$pass		=	"***REMOVED***";
$database 	=	"hypersignal";

$fbappid	=	'***REMOVED***';
$fbsecret	=	'***REMOVED***';

$siteurl	=	"http://localhost/hypersignal/WebSite/";
$apiurl		=	"http://localhost/hypersignal/WebService/";

$replacelist=	array("SITEURL" => $siteurl, "APIURL" => $apiurl);

if(!mysql_connect($host,$user,$pass)) 
	die(mysql_error());

if(!mysql_select_db($database)) 
	die(mysql_error());

include("progconfig.php");	


?>
