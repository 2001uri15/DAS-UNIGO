<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Parámetros de conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

try {
    // Crear conexión con charset UTF-8
    $dsn = "mysql:host=$host;dbname=$dbname;charset=utf8mb4";
    $conn = new PDO($dsn, $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Configuraciones adicionales para UTF-8
    $conn->exec("SET NAMES 'utf8mb4'");
    $conn->exec("SET CHARACTER SET utf8mb4");
    $conn->exec("SET COLLATION_CONNECTION = 'utf8mb4_unicode_ci'");
    
    // Manejar solicitud GET
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $sql = "SELECT DISTINCT `stop_name` FROM `bus_stop` ORDER BY `stop_name` ASC";
        $stmt = $conn->prepare($sql);
        $stmt->execute();
        
        $stop_names = $stmt->fetchAll(PDO::FETCH_COLUMN);
        
        // Convertir cada elemento a UTF-8 si es necesario
        $stop_names = array_map(function($name) {
            // Verificar si el string ya está en UTF-8
            if (!mb_check_encoding($name, 'UTF-8')) {
                // Si no está en UTF-8, intentar convertir desde ISO-8859-1
                $name = utf8_encode($name);
            }
            return $name;
        }, $stop_names);
        
        http_response_code(200);
        echo json_encode($stop_names, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(array("message" => "Error de base de datos: " . $e->getMessage()), JSON_UNESCAPED_UNICODE);
}
?>
