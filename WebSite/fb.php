<?
$loadfb = true;
include("includes/system.php");
if($_REQUEST["frame"] == 1)	
	header("Location: $siteurl");
echo $loginSection; 
