<?php

class TelegramBotAPI
{
	private $_baseUrl;

	public function __construct( $token )
	{
		$this->_baseUrl = "https://api.telegram.org/bot$token/";
	}
	
	public function __call( $method, $args ) 
	{
		$postfields = count( $args ) ? $args[0] : '';
		
		$curl = curl_init();

		curl_setopt_array( $curl, 
			[
				CURLOPT_URL            => $this->_baseUrl . $method,
				CURLOPT_POST           => 1,
				CURLOPT_POSTFIELDS     => $postfields,
				CURLOPT_RETURNTRANSFER => true,
				CURLOPT_HEADER     	   => false,
				CURLOPT_TIMEOUT        => 1,
				CURLOPT_FOLLOWLOCATION => true,
				CURLOPT_MAXREDIRS      => 2				
			]
		);
		
		$json = json_decode( curl_exec( $curl ), true );
		curl_close( $curl );
		
		return $json;
	}
}

?>
