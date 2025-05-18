<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Parámetros de conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

// Obtener datos del POST
$data = json_decode(file_get_contents("php://input"));

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Preparar consulta SQL
    $sql = "INSERT INTO bus_stop (
        stop_id, stop_code, stop_name, stop_desc, stop_lat, stop_lon, 
        zone_id, stop_url, location_type, parent_station, 
        stop_timezone, wheelchair_boarding, platform_code, level_id
    ) VALUES (
        :stop_id, :stop_code, :stop_name, :stop_desc, :stop_lat, :stop_lon, 
        :zone_id, :stop_url, :location_type, :parent_station, 
        :stop_timezone, :wheelchair_boarding, :platform_code, :level_id
    ) ON DUPLICATE KEY UPDATE
        stop_code = VALUES(stop_code),
        stop_name = VALUES(stop_name),
        stop_desc = VALUES(stop_desc),
        stop_lat = VALUES(stop_lat),
        stop_lon = VALUES(stop_lon),
        zone_id = VALUES(zone_id),
        stop_url = VALUES(stop_url),
        location_type = VALUES(location_type),
        parent_station = VALUES(parent_station),
        stop_timezone = VALUES(stop_timezone),
        wheelchair_boarding = VALUES(wheelchair_boarding),
        platform_code = VALUES(platform_code),
        level_id = VALUES(level_id)";
    
    $stmt = $conn->prepare($sql);
    
    // Bind parameters
    $stmt->bindParam(':stop_id', $data->stop_id);
    $stmt->bindParam(':stop_code', $data->stop_code);
    $stmt->bindParam(':stop_name', $data->stop_name);
    $stmt->bindParam(':stop_desc', $data->stop_desc);
    $stmt->bindParam(':stop_lat', $data->stop_lat);
    $stmt->bindParam(':stop_lon', $data->stop_lon);
    $stmt->bindParam(':zone_id', $data->zone_id);
    $stmt->bindParam(':stop_url', $data->stop_url);
    $stmt->bindParam(':location_type', $data->location_type);
    $stmt->bindParam(':parent_station', $data->parent_station);
    $stmt->bindParam(':stop_timezone', $data->stop_timezone);
    $stmt->bindParam(':wheelchair_boarding', $data->wheelchair_boarding);
    $stmt->bindParam(':platform_code', $data->platform_code);
    $stmt->bindParam(':level_id', $data->level_id);
    
    // Ejecutar consulta
    if($stmt->execute()) {
        http_response_code(200);
        echo json_encode(array("message" => "Parada actualizada correctamente."));
    } else {
        http_response_code(503);
        echo json_encode(array("message" => "No se pudo actualizar la parada."));
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(array("message" => "Error de base de datos: " . $e->getMessage()));
}
?>
