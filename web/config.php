<?php

// токен бота
define( 'TOKEN', 'xxxxxxxxxxxxxxxxxxxx' );

// URL-адрес вебхука
define( 'WEBHOOK', 'https://domain.com/L2TelegramBot/webhook.php?action=update' );

// айди вашего тг аккаунта
define( 'USER_ID', '123123123' );

// секретный ключ, который будет присылатся в заголовке X-Telegram-Bot-Api-Secret-Token от тг серверов
define( 'SECRET', '1234567' );

$config = [
	'auth' => [
		'driver'	=> 'mysql',
		'dbhost'	=> 'localhost',
		'dbuser'	=> 'root',
		'dbpass'	=> 'root',
		'dbname'	=> 'l2jdb',
		'dbport'	=> 3306,
		'charset'	=> 'utf8'
	],
	'game' => [
		'driver'	=> 'mysql',
		'dbhost'	=> 'localhost',
		'dbuser'	=> 'root',
		'dbpass'	=> 'root',
		'dbname'	=> 'l2jdb',
		'dbport'	=> 3306,
		'charset'	=> 'utf8'
	]
];

?>