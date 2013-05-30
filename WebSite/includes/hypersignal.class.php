<?

class Statement_Result
{
	private $_bindVarsArray = array();
	private $_results = array();

	public function __construct(&$stmt)
	{
		$meta = $stmt->result_metadata();

		while ($columnName = $meta->fetch_field())
			$this->_bindVarsArray[] = &$this->_results[$columnName->name];

		call_user_func_array(array($stmt, 'bind_result'), $this->_bindVarsArray);

		$meta->close();
	}

	public function Get_Array()
	{
		return $this->_results;
	}

	public function Get($column_name)
	{
		return $this->_results[$column_name];
	}
}


class HyperSignal	{

	/*	Select Queries	*/
	private $q_finduser			=	"SELECT * FROM `users` WHERE `uid` = ?";
	private $q_getdevices		=	"SELECT DISTINCT `device`,`manufacturer`,`model`,`brand`,`android`,`release` FROM `devices`";
	private $q_gettopsender		=	"SELECT `name`,`sentkm` FROM `users` WHERE `name` != 'Anonymous' ORDER BY `sentkm` DESC LIMIT 5";

	/*	Insert Queries	*/
	private $q_adduser			=	"INSERT INTO `users` VALUES(?, ?, ?, ?, CURDATE(), ?, 0, ?, ?, NOW())";

	/*	Update Queries	*/
	private $q_updateuserlog	=	"UPDATE `users` SET `lastip` = ?, lastaccess = NOW() WHERE `uid` = ?";


	function __construct($host,$user,$pass,$database,$prog,$basepath,$lang) {
		/*	Constrói o objeto	*/
		$this->conn				=	new mysqli($host,$user,$pass,$database);
		$this->prog				=	$prog;
		$this->basepath			=	$basepath;
		$this->lang				=	$lang;

		$this->langdata			=	$this->LoadLangData();
	}

	function loadTPL($file)	{
		$filename = $this->basepath."/".$this->lang."/".$file;
		if(file_exists($filename))	{
			$handler				=	fopen($filename,"r");
			$text					=	fread($handler,filesize($filename));
		}else{
			$filename = $this->basepath."/default/".$file;
			$handler				=	fopen($filename,"r");
			$text					=	fread($handler,filesize($filename));
		}
		fclose($handler);
		return $text;
	}
	function LoadLangData()	{
		$langdata = $this->loadTPL("keywords.json");
		return json_decode($langdata);
	}
	function ParseLanguage($text)	{
		foreach($this->langdata as $key => $value)
			$text = str_ireplace("[".$key."]",$value,$text);
		return $text;
	}
	function ReplaceList($string,$list)	{
		foreach($list as $key => $value)	
			$string	=	str_ireplace("{".$key."}",$value,$string);
		return $string;		
	}

	function checkUser($uid) {
		/*	Checa se um usuário existe	*/
		$cmd					=	$this->conn->stmt_init();
		$cmd->prepare($this->q_finduser);
		$cmd->bind_param('s', $uid);
		$cmd->execute();	
		$cmd->store_result();
		$ok 					=	$cmd->num_rows > 0;
		$cmd->close();
		return $ok;
	}
	function addUser($uid, $username, $name, $email, $ip, $location) {
		/*	Adiciona um usuário	*/
		$locale = explode(",",utf8_decode($location));
		$cmd					=	$this->conn->stmt_init();
		$cmd->prepare($this->q_adduser);

		$cmd->bind_param('issssss', $uid,$username,$name,$email,$ip,$locale[0],$locate[1]);
		$ok = $cmd->execute();	
		$cmd->close();
		return $ok;
	}
	function updateUserEnter($uid, $ip) {
		/*	Atualiza a entrada do usuário no site	*/
		$cmd					=	$this->conn->stmt_init();
		$cmd->prepare($this->q_updateuserlog);
		$cmd->bind_param('ss', $ip,$uid);
		$ok = $cmd->execute();	
		$cmd->close();
		return $ok;
	}
	function getDevices() {
		/*	Pega a lista de dispositívos cadastrados	*/
		$cmd					=	$this->conn->stmt_init();
		$cmd->prepare($this->q_getdevices);
		$cmd->execute();	
		$cmd->store_result();
	
		$result 	= 	new Statement_Result($cmd);
		$results	=	array();
		if($cmd->num_rows() > 0)	{
			while($cmd->fetch())	{
				$x = $result->Get_Array();
				$x = unserialize(serialize($x));
				$results[] = $x;			
			}
		}
		$cmd->close();
		return $results;
	}
	function getTopList() {
		/*	Pega o TOP5 de contribuidores do SignalTracker	*/
		$cmd					=	$this->conn->stmt_init();
		$cmd->prepare($this->q_gettopsender);
		$cmd->execute();	
		$cmd->store_result();
	
		$result 	= 	new Statement_Result($cmd);
		$results	=	array();
		if($cmd->num_rows() > 0)	{
			while($cmd->fetch())	{
				$x = $result->Get_Array();
				$x = unserialize(serialize($x));
				$results[] = $x;	
			}		
		}
		$cmd->close();

		return $results;
	}
}
