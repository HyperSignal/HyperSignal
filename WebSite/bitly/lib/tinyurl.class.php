<?php
require_once('bitly.class.php');
class Tinyurl extends Bitly {
    function Tinyurl()
    {
    	$this->api = 'http://tinyurl.com/';
    }
    
    public function shortenSingle($url)
    {
    	return $this->process('api-create.php', '&url=' . $url);
    }
}
?>
