<?php

class DB
{
	private $_dbh = null;
	
	public function __construct( $config )
	{
		$driver		= $config['driver'];
		$dbhost		= $config['dbhost']; 
		$dbport		= $config['dbport']; 
		$dbuser		= $config['dbuser']; 
		$dbpass		= $config['dbpass']; 
		$dbname		= $config['dbname'];
		$charset	= $config['charset'];
		
		$availableDrivers = \PDO::getAvailableDrivers();
		
		if ( !in_array( $driver, $availableDrivers ) )
		{
			throw new \PDOException("Driver $driver not supported!");
		}
		
		$dsn = "$driver:host=$dbhost;port=$dbport;dbname=$dbname;charset=$charset";
		$options = [
			\PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
			\PDO::ATTR_ERRMODE 			  => \PDO::ERRMODE_EXCEPTION
		];
		
		try 
		{
			$this->_dbh = new \PDO( $dsn, $dbuser, $dbpass, $options );
		}
		catch ( \PDOException $e )
		{
			throw $e;
		}
	}
	
	public function prepareAndExecute( $sql, array $parameters = null, $bind = true, $bindValue = true )
	{
		try 
		{
			if ( $stmt = $this->_dbh->prepare( $sql ) ) 
			{
				if ( $bind && is_array( $parameters ) ) 
				{
					foreach ( $parameters as $key => $value ) 
					{
						$parameter = is_numeric( $key ) ? $key + 1 : $key;
						$bindValue 
							? $stmt->bindValue( $parameter, $value )
							: $stmt->bindParam( $parameter, $value );
					}
				}
		
				if ( $stmt->execute( $bind ? null : $parameters ) ) 
				{
					return $stmt;
				}
			}
		} 
		catch ( \PDOException $e ) 
		{
			throw $e;
		}
		
		return false;
	}
	
	public function fetch( $sql, array $parameters = null, $bind = true, $bindValue = true )
	{
		if ( $stmt = $this->prepareAndExecute( $sql, $parameters, $bind, $bindValue ) ) 
		{
			try 
			{
				if ( $result = $stmt->fetch() )
				{
					return $result;
				}
			} 
			catch ( \PDOException $e ) 
			{
				throw $e;
			}
		}

		return false;
	}
	
	public function fetchAll( $sql, array $parameters = null, $bind = true, $bindValue = true )
	{
		if ( $stmt = $this->prepareAndExecute( $sql, $parameters, $bind, $bindValue ) ) 
		{
			try 
			{
				if ( $results = $stmt->fetchAll() )
				{
					return $results;
				}
			} 
			catch ( \PDOException $e ) 
			{
				throw $e;
			}
		}
	
		return false;
	}
}

?>