<?php
/*
SOAP server
Author: Ruslanas Balčiūnas
http://bitly.googlecode.com
*/
ini_set("soap.wsdl_cache_enabled", "0");
include('lib/bitly.class.php');

class BitlyService { 
    
    function getShortened($url) {
        include('config.php');
        $bitly = new Bitly($login, $apiKey);
        return $bitly->shortenSingle($url);
    }
}

$server = new SoapServer("http://ruslanas.com/bitly/bitly.wsdl");
$server->setClass("BitlyService");
$server->handle();
?>
