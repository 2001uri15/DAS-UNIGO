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

// Inicializar respuesta
$response = [
    'status' => '',
    'data' => [],
    'message' => ''
];

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $conn->exec("SET NAMES 'utf8'");
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $lat = isset($_GET['lat']) ? $_GET['lat'] : '';
        $log = isset($_GET['log']) ? $_GET['log'] : '';
        
        if (empty($lat) || empty($log)) {
            http_response_code(400);
            $response['status'] = 'error';
            $response['message'] = 'Los parámetros lat (latitud) y log (longitud) son requeridos.';
            echo json_encode($response, JSON_UNESCAPED_UNICODE);
            exit;
        }
        
        // Consulta para encontrar la parada más cercana
        $sql = "SELECT 
                    stop_id,
                    stop_name,
                    stop_lat,
                    stop_lon,
                    (6371 * ACOS(
                        COS(RADIANS(:lat)) * COS(RADIANS(stop_lat)) * 
                        COS(RADIANS(stop_lon) - RADIANS(:log)) + 
                        SIN(RADIANS(:lat)) * SIN(RADIANS(stop_lat))
                    )) AS distance
                FROM 
                    bus_stop
                ORDER BY 
                    distance ASC
                LIMIT 1";
        
        $stmt = $conn->prepare($sql);
        $stmt->bindParam(':lat', $lat);
        $stmt->bindParam(':log', $log);
        $stmt->execute();
        
        $parada = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($parada) {
            http_response_code(200);
            $response['status'] = 'success';
            $response['data'] = [
                'stop_id' => $parada['stop_id'],
                'stop_name' => $parada['stop_name'],
                'stop_lat' => $parada['stop_lat'],
                'stop_lon' => $parada['stop_lon'],
                'distance_km' => $parada['distance']
            ];
        } else {
            http_response_code(404);
            $response['status'] = 'not_found';
            $response['message'] = 'No se encontraron paradas de autobús en la base de datos.';
        }
        
        echo json_encode($response, JSON_UNESCAPED_UNICODE);
        exit;
    }
} catch(PDOException $e) {
    http_response_code(500);
    $response['status'] = 'error';
    $response['message'] = "Error de base de datos: " . $e->getMessage();
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
}
?>
