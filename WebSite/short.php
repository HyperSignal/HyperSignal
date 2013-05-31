<?
include("includes/config.php");
include("includes/bitly.class.php");

if(isset($_REQUEST["URL"]))
	print json_encode(bitly_v3_shorten($_REQUEST["URL"]));
