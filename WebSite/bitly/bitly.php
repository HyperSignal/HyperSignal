<?php
/*
Author: Ruslanas Balčiūnas
http://bitly.googlecode.com
*/

function errorHandler($errno, $errstr, $errfile, $errline)
{
    header('Content-Type: text/plain; charset="utf-8"');
    $ret = array(
        "errorCode" => 10001,
        "errorMessage" => $errstr,
        "statusCode" => "ERROR"
        );
    echo json_encode($ret);
    exit;
}

$oldErrorHandler = set_error_handler('errorHandler');

include('config.php');
include('lib/bitly.class.php');

$bitly = new Bitly($login, $apiKey);
$action = $_POST['action'];
$url = $_POST['url'];

if( $bitly->validAction($action)) {
    echo $bitly->{$action}($url);
} else {
    echo $bitly->error();
}
?>
