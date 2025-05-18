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
    
    if ($_SERVER['REQUEST_METHOD'] === 'GET' || $_SERVER['REQUEST_METHOD'] === 'POST') {
        $fecha = isset($_GET['fecha']) ? $_GET['fecha'] : '';
        
        if (empty($fecha)) {
            http_response_code(400);
            $response['status'] = 'error';
            $response['message'] = 'Parámetro fecha es requerido.';
            echo json_encode($response, JSON_UNESCAPED_UNICODE);
            exit;
        }
        
        // Primera llamada al procedimiento para obtener las rutas
        $sql = "CALL ObtenerRutasCementerioPorFecha(:fecha)";
        $stmt = $conn->prepare($sql);
        $stmt->bindParam(':fecha', $fecha);
        $stmt->execute();
        
        $rutas = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Si no hay resultados, verificar si hay un segundo result set con mensaje
        if (empty($rutas)) {
            // Avanzar al siguiente result set si existe
            if ($stmt->nextRowset()) {
                $mensaje = $stmt->fetch(PDO::FETCH_ASSOC);
                if ($mensaje && isset($mensaje['Mensaje'])) {
                    http_response_code(200);
                    $response['status'] = 'success';
                    $response['message'] = $mensaje['Mensaje'];
                    echo json_encode($response, JSON_UNESCAPED_UNICODE);
                    exit;
                }
            }
            
            http_response_code(404);
            $response['status'] = 'not_found';
            $response['message'] = 'No se encontraron rutas al cementerio para la fecha proporcionada.';
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
