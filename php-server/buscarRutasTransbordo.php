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
        $origen = isset($_GET['origen']) ? $_GET['origen'] : '';
        $destino = isset($_GET['destino']) ? $_GET['destino'] : '';
        $fecha = isset($_GET['fecha']) ? $_GET['fecha'] : '';
        $hora = isset($_GET['hora']) ? $_GET['hora'] : '';
        
        if (empty($origen) || empty($destino) || empty($fecha) || empty($hora)) {
            http_response_code(400);
            $response['status'] = 'error';
            $response['message'] = 'Parámetros incompletos. Se requieren origen, destino, fecha y hora.';
            echo json_encode($response, JSON_UNESCAPED_UNICODE);
            exit;
        }
        
        $sql = "CALL BuscarRutasConTransbordo(:origen, :destino, :fecha, :hora)";
        $stmt = $conn->prepare($sql);
        $stmt->bindParam(':origen', $origen);
        $stmt->bindParam(':destino', $destino);
        $stmt->bindParam(':fecha', $fecha);
        $stmt->bindParam(':hora', $hora);
        $stmt->execute();
        
        $rutas = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        if (empty($rutas)) {
            http_response_code(404);
            $response['status'] = 'not_found';
            $response['message'] = 'No se encontraron rutas directas para los parámetros proporcionados.';
            echo json_encode($response, JSON_UNESCAPED_UNICODE);
            exit;
        }
        
        http_response_code(200);
        $response['status'] = 'success';
        $response['data'] = $rutas;
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
