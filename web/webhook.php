<?php

ini_set( 'display_errors', '0' );
error_reporting( E_ALL );

require 'config.php';
require 'functions.php';
require 'TelegramBotApi.php';

$api = new TelegramBotApi( TOKEN ); 

$action = $_REQUEST['action'] ?? '';

header( 'Content-type: application/json' );

if ( $action == 'setWebhook' || $action == 'getWebhook' )
{
    $webhookInfo = $api->getWebhookInfo();

    if ( $action == 'getWebhook' || isset( $webhookInfo['error_code'] ) )
    {
        die( json_encode( $webhookInfo ) );
    }

	$url = $webhookInfo['result']['url'] ?? '';
	
    if ( $url <> WEBHOOK )
    {
        $result = $api->setWebhook([ 
			'url' => WEBHOOK,
			'allowed_updates' => json_encode([ 
				'message',
				//'chat_join_request',
				//'chat_member',
				'callback_query',
				//'my_chat_member'
			]),
			'secret_token' => SECRET
		]);

		die( json_encode( $result ) );
    }

    die( json_encode( [ 'ok' => true, 'result' => true, 'description' => 'Webhook update is not required!' ] ) );
}
else if ( $action == 'update' )
{
	// get_headers()['X-Telegram-Bot-Api-Secret-Token'] 
	$secret = $_SERVER['HTTP_X_TELEGRAM_BOT_API_SECRET_TOKEN'] ?? '';
	
	$update = json_decode( file_get_contents( 'php://input' ), true );
	
	$chatId = $update['message']['chat']['id']
		?? $update['message']['from']['id'] 
		?? $update['callback_query']['from']['id']
		?? '';
		
	if ( $secret <> SECRET || $chatId <> USER_ID )
	{
		$api->sendMessage([ 
			'chat_id' => USER_ID, 
			'text' => "<b>Warning</b>: Unauthorized access!\n\n<b>Update</b>: " . print_r( $update, true ), 
			'parse_mode' => 'html'
		]);
		
		die( json_encode( [ 'ok' => false, 'result' => false, 'description' => 'Unauthorized access!' ] ) );
	}
	
	$messageId = $update['message']['message_id'] 
		?? $update['callback_query']['message']['message_id'] 
		?? '';
	
	$messageText = $update['message']['text'] 
		?? $update['callback_query']['data'] 
		?? '';

	$command = explode( ' ', $messageText );

	$whitelistCommands = [
		'/start',
		'/help',
		'/menu',
		'/add_item', 
		'/online', 
		'/statistics',
		'/items_delayed_status',
		'/restart',
		'/shutdown',
		'/shutdown_abort'
	];

	if ( in_array( $command[0], [ $whitelistCommands[0], $whitelistCommands[1], $whitelistCommands[2] ] ) )
	{
		$text = '<b>Сommand list</b>';
		$text .= "\n\n/add_item {char name} {item id} {item count}";
		$text .= "\n/online";
		$text .= "\n/statistics";
		$text .= "\n/items_delayed_status";
		$text .= "\n/shutdown {seconds}";
		$text .= "\n/restart {seconds}";
		$text .= "\n/shutdown_abort";
		
		/*$keyboard = [ 
			//'inline_keyboard' => [ 
			'keyboard' => [ 
				[ 
					[ 
						'text' => '123', 'callback_data' => '/test' 
					] 
				] 
			] 
		];*/
		
		$api->sendMessage([ 
			'chat_id' => USER_ID, 
			'text' => $text,
			'parse_mode' => 'html',
			//'reply_markup' => json_encode( $keyboard )
		]);
	}
	else if ( in_array( $command[0], $whitelistCommands ) ) 
	{
		require 'db.class.php';
		
		try
		{
			$authDb = new DB( $config['auth'] );
			$gameDb = new DB( $config['game'] );
			
			if ( $command[0] == '/add_item' )
			{
				$result = 'Syntax error!';
				
				if ( count( $command ) == 4 )
				{
					if ( $char = $gameDb->fetch( "SELECT `obj_Id` FROM `characters` WHERE `char_name` = ?", [ $command[1] ] ) )
					{
						$sql = "INSERT INTO `items_delayed` ( `owner_id`, `item_id`, `count`, `payment_status`, `description` ) VALUES ( ?, ?, ?, 0, 'Telegram Bot' )";
						
						if ( $gameDb->prepareAndExecute( $sql, [ $char['obj_Id'], $command[2], $command[3] ] )->rowCount() )
						{
							$result = 'Successfully!';
						}
					}
					else
					{
						$result = "Character {$command[1]} not found!";
					}
				}
				
				$api->sendMessage([ 
					'chat_id' => USER_ID, 
					'text' => $result, 
					'parse_mode' => 'html'
				]);
			}
			else if( $command[0] == '/online' )
			{
				$stmt = $gameDb->prepareAndExecute( "SELECT COUNT(*) FROM characters WHERE online = '1'" );

				$api->sendMessage([ 
					'chat_id' => USER_ID, 
					'text' => 'Current online: <b>' . num_format( $stmt ? $stmt->fetchColumn() : 0 ) . '</b>', 
					'parse_mode' => 'html'
				]);
			}
			else if ( $command[0] == '/statistics' )
			{
				$stmtAccs = $authDb->prepareAndExecute( "SELECT COUNT(*) FROM accounts" );
				//$totalChars = ( $stmtChars = $gameDb->prepareAndExecute( "SELECT COUNT(*) FROM characters" ) ) ? $stmtChars->fetchColumn() : 0;
				$totalChars = ( $stmtChars = $gameDb->prepareAndExecute( "SELECT COUNT(*) FROM character_subclasses WHERE isBase = '1'" ) ) ? $stmtChars->fetchColumn() : 0;
				$stmtClans = $gameDb->prepareAndExecute( "SELECT COUNT(*) FROM clan_data" );
				$stmtAllies = $gameDb->prepareAndExecute( "SELECT COUNT(*) FROM ally_data" );
				$stmtHeroes = $gameDb->prepareAndExecute( "SELECT COUNT(*) FROM heroes" );
				
				$totalHuman = ( $stmtHumman = $gameDb->prepareAndExecute("
					SELECT 
						COUNT(*)
					FROM 
						`character_subclasses` cs
					JOIN
						`char_templates` ct ON ( ct.RaceId = 0 AND ct.ClassId = cs.class_id AND cs.isBase = 1 );
				")) ? $stmtHumman->fetchColumn() : 0;
				
				$totalElf = ( $stmtElf = $gameDb->prepareAndExecute("
					SELECT 
						COUNT(*)
					FROM 
						`character_subclasses` cs
					JOIN
						`char_templates` ct ON ( ct.RaceId = 1 AND ct.ClassId = cs.class_id AND cs.isBase = 1 );
				")) ? $stmtElf->fetchColumn() : 0;

				$totalDarkElf = ( $stmtDarkElf = $gameDb->prepareAndExecute("
					SELECT 
						COUNT(*)
					FROM 
						`character_subclasses` cs
					JOIN
						`char_templates` ct ON ( ct.RaceId = 2 AND ct.ClassId = cs.class_id AND cs.isBase = 1 );
				")) ? $stmtDarkElf->fetchColumn() : 0;
				
				$totalOrc = ( $stmtOrc = $gameDb->prepareAndExecute("
					SELECT 
						COUNT(*)
					FROM 
						`character_subclasses` cs
					JOIN
						`char_templates` ct ON ( ct.RaceId = 3 AND ct.ClassId = cs.class_id AND cs.isBase = 1 );
				")) ? $stmtOrc->fetchColumn() : 0;
				
				$totalDwarf = ( $stmtDwarf = $gameDb->prepareAndExecute("
					SELECT 
						COUNT(*)
					FROM 
						`character_subclasses` cs
					JOIN
						`char_templates` ct ON ( ct.RaceId = 4 AND ct.ClassId = cs.class_id AND cs.isBase = 1 );
				")) ? $stmtDwarf->fetchColumn() : 0;
				
				$totalKamael = ( $stmtKamael = $gameDb->prepareAndExecute("
					SELECT 
						COUNT(*)
					FROM 
						`character_subclasses` cs
					JOIN
						`char_templates` ct ON ( ct.RaceId = 5 AND ct.ClassId = cs.class_id AND cs.isBase = 1 );
				")) ? $stmtKamael->fetchColumn() : 0;

				$sevenSignsStatus = $gameDb->fetch( "SELECT * FROM seven_signs_status" );

				$activePeriod = '';
				
				switch ( $sevenSignsStatus['active_period'] ) {
					case 0: 
						$activePeriod = "Подготовка к битве за Семь Печатей."; 
						break;
					case 1: 
						$activePeriod = "Период ивента. Поговорите со Жрецом Рассвета или Жрицей Заката."; 
						break;
					case 2: 
						$activePeriod = "Ивент завершен. Собираются данные статистики."; 
						break;
					case 3: 
						$activePeriod = "Период действия Печати. Следующий ивент будет проведен в следующий понеделник."; 
						break;
				}
				
				switch ( $sevenSignsStatus['active_period'] ) {
					case 0:
					case 2:
						$activePeriod .= ' (До 18:00)';
						break;
					case 1:
					case 3:
						$activePeriod .= ' (До 18:00 следующего понедельника)';
						break;
				}

				$duskTotalScore = ( ( $sevenSignsStatus['dusk_stone_score'] / ( $totalStoneScore ? $totalStoneScore : 1 ) ) * 500 ) + $sevenSignsStatus['dusk_festival_score'];
				$dawnTotalScore = ( ( $sevenSignsStatus['dawn_stone_score'] / ( $totalStoneScore ? $totalStoneScore : 1 ) ) * 500 ) + $sevenSignsStatus['dawn_festival_score'];
				
				$totalOverallScore = $duskTotalScore + $dawnTotalScore;
				
				if ( $totalOverallScore ) 
				{
				  $dawnPercent = round( ( $dawnTotalScore / $totalOverallScore ) * 100 );
				  $duskPercent = round( ( $duskTotalScore / $totalOverallScore ) * 100 );
				} 
				else
				{
					$duskPercent = 0;
					$dawnPercent = 0;
				}

				$text = "<b>Статистика</b>";
				$text .= "\n\nАккаунтов: <b>" . num_format( $stmtAccs ? $stmtAccs->fetchColumn() : 0 ) . '</b>';
				$text .= "\n\nПерсонажей: <b>" . num_format( $totalChars ) . '</b>';
				$text .= "\n - Людей: <b>" . num_format( $totalHuman ) . '</b> (' . round( $totalHuman / ( $totalChars / 100 ) ) . '%)';
				$text .= "\n - Эльфы: <b>" . num_format( $totalElf ) . '</b> (' . round( $totalElf / ( $totalChars / 100 ) ) . '%)';
				$text .= "\n - Темные Эльфы: <b>" . num_format( $totalDarkElf ) . '</b> (' . round( $totalDarkElf / ( $totalChars / 100 ) ) . '%)';
				$text .= "\n - Орки: <b>" . num_format( $totalOrc ) . '</b> (' . round( $totalOrc / ( $totalChars / 100 ) ) . '%)';
				$text .= "\n - Гномы: <b>" . num_format( $totalDwarf ) . '</b> (' . round( $totalDwarf / ( $totalChars / 100 ) ) . '%)';
				$text .= "\n - Камаэли: <b>" . num_format( $totalKamael ) . '</b> (' . round( $totalKamael / ( $totalChars / 100 ) ) . '%)';
				
				$text .= "\n\nКланов: <b>" . num_format( $stmtClans ? $stmtClans->fetchColumn() : 0 ) . '</b>';
				$text .= "\nАльянсов: <b>" . num_format( $stmtAllies ? $stmtAllies->fetchColumn() : 0 ) . '</b>';
				$text .= "\nГероев: <b>" . num_format( $stmtHeroes ? $stmtHeroes->fetchColumn() : 0 ) . '</b>';
				
				$text .= "\n\n<b>Cемь печатей:</b>";
				$text .= "<pre>\t\t" . $activePeriod . '</pre>';		
				$text .= "\n - Рассвет / Dawn: <b>" . $dawnPercent . '%</b>';				
				$text .= "\n - Закат / Dusk: <b>" . $duskPercent . '%</b>';
				
				$api->sendMessage([ 
					'chat_id' => USER_ID, 
					'text' => $text,
					'parse_mode' => 'html'
				]);
			} 
			else if ( $command[0] == '/items_delayed_status' )
			{
				$entities = $gameDb->fetchAll("
					SELECT 
						id.*,
						c.char_name
					FROM 
						`items_delayed` id
					JOIN 
						`characters` c ON ( c.obj_Id = id.owner_id )
					ORDER BY
						id.id DESC
				");

				$text .= "\n\n<b>Выдача предметов:</b>\n<pre>";
				if ( is_array( $entities ) )
				{
					$text .= '|' . /*column( 'Char name', true ) .*/ column( 'Item Id' ) . column( 'Count' ) . column( 'Description' ) . column( 'Status' ) . "\n";
					foreach ( $entities as $entity )
					{
						$text .= '|';
						//$text .= column( $entity['char_name'], true );
						$text .= column( $entity['item_id'] );
						$text .= column( $entity['count'] );
						$text .= column( $entity['description'] );
						$text .= column( $entity['payment_status'] ? "yes" : "no" ) . "\n";
					}
				}
				$text .= '</pre>';
				
				$api->sendMessage([ 
					'chat_id' => USER_ID, 
					'text' => $text,
					'parse_mode' => 'html'
				]);
			} else if ( $command[0] == '/restart' || $command[0] == '/shutdown' ) {
			    $sql = "INSERT INTO `delayed_tasks` ( `event`, `args` ) VALUES ( ?, ? )";

                $rowCount = $gameDb->prepareAndExecute( $sql, [ substr( $command[0], 1 ), $command[1] ] )->rowCount();

                $api->sendMessage([
                	'chat_id' => USER_ID,
                	'text' => $rowCount ? 'Successfully!' : 'Failed!',
                	'parse_mode' => 'html'
                ]);
			} else if ( $command[0] == '/shutdown_abort' ) {
			    $sql = "INSERT INTO `delayed_tasks` ( `event`, `args` ) VALUES ( ?, '' )";

			    $rowCount = $gameDb->prepareAndExecute( $sql, [ 'abort' ] )->rowCount();

                $api->sendMessage([
                    'chat_id' => USER_ID,
                    'text' => $rowCount ? 'Successfully!' : 'Failed!',
                    'parse_mode' => 'html'
                    ]);
            }
		}
		catch ( \PDOException $e )
		{
			$api->sendMessage([ 
				'chat_id' => USER_ID, 
				'text' => 'Exception: ' . $e->getMessage(), 
				'parse_mode' => 'html'
			]);
		}
	}
	else
	{
		$api->sendMessage([ 
			'chat_id' => USER_ID, 
			'text' => "<b>Warning</b>: Unregistered command!\n\n<b>Update</b>: " . print_r( $update, true ), 
			'parse_mode' => 'html'
		]);
	}
}

echo json_encode( [] );

?>