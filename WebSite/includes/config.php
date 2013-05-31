<?

$host		= 	"localhost";
$user		=	"root";
$pass		=	"***REMOVED***";
$database 	=	"hypersignal";

$fbappid	=	'***REMOVED***';
$fbsecret	=	'***REMOVED***';

$siteurl	=	"http://localhost/hypersignal/WebSite/";
$apiurl		=	"http://localhost/hypersignal/WebService/";
$basepath	=	"/var/www/hsbase/";

$bitly_apiKey	=	'***REMOVED***';
$bitly_login	=	'***REMOVED***';
$bitly_clientid	=	'***REMOVED***';
$bitly_secret	=	'***REMOVED***';

define('bitlyKey', $bitly_apiKey);
define('bitlyLogin' , $bitly_login);
define('bitly_clientid' , $bitly_clientid);
define('bitly_secret' , $bitly_secret);

$lang = substr($_SERVER['HTTP_ACCEPT_LANGUAGE'], 0, 2);

$replacelist=	array("SITEURL" => $siteurl, "APIURL" => $apiurl);

if(!mysql_connect($host,$user,$pass)) 
	die(mysql_error());

if(!mysql_select_db($database)) 
	die(mysql_error());

include("progconfig.php");	


?>
