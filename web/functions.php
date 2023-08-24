<?php

function num_format( $value ) 
{
	return number_format( $value, 0, ',', ' ' );
}

function column( $text, $first = false, $length = 26 ) {
	$textLen = mb_strlen( $text );
	
	if ( $first )
		return $text . str_repeat( ' ', $length - $textLen ) . ' |';
	
	return str_repeat( ' ', ( $length / 2 ) - $textLen ) . $text . ' |';
}

?>