<?php
require 'fcm_config.php';
require 'fcm_sender.php';

// Conexión a la base de datos
$servername = "localhost";
$username = "Xalarrazabal025"; 
$password = "vRN7UMCCFV"; 
$dbname = "Xalarrazabal025_unigo"; 

$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die("Error de conexión: " . $conn->connect_error);
}

// Procesar envío si se ha enviado el formulario
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $titulo = $_POST['titulo'] ?? '';
    $mensaje = $_POST['mensaje'] ?? '';
    $destino = $_POST['destino'] ?? '';
    $token_individual = $_POST['token_individual'] ?? '';
    
    try {
        $fcm = new FCMSender(FCM_SERVICE_ACCOUNT_PATH);
        $response = null;
        
        if ($destino === 'individual' && !empty($token_individual)) {
            // Enviar a dispositivo individual
            $response = $fcm->sendToDevice(
                $token_individual,
                $titulo,
                $mensaje,
                ['fecha' => date('Y-m-d H:i:s')]
            );
        } elseif ($destino === 'todos') {
            // Obtener todos los tokens de la base de datos
            $sql = "SELECT `tokenFCM` FROM `app_tokenFCM` WHERE `tokenFCM` IS NOT NULL";
            $result = $conn->query($sql);
            
            if ($result->num_rows > 0) {
                $tokens = [];
                while ($row = $result->fetch_assoc()) {
                    $tokens[] = $row['tokenFCM'];
                }
                
                // Enviar a cada token
                foreach ($tokens as $token) {
                    $response = $fcm->sendToDevice(
                        $token,
                        $titulo,
                        $mensaje,
                        ['fecha' => date('Y-m-d H:i:s')]
                    );
                }
            } else {
                throw new Exception("No se encontraron tokens en la base de datos");
            }
        }
        
        // Mostrar resultado
        if ($response) {
            echo "<div class='alert alert-success'>Notificación enviada con éxito!</div>";
            echo "<pre>" . print_r($response, true) . "</pre>";
        }
        
    } catch (Exception $e) {
        echo "<div class='alert alert-danger'>Error: " . $e->getMessage() . "</div>";
    }
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel de Notificaciones FCM</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { padding: 20px; background-color: #f8f9fa; }
        .form-container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 15px rgba(0,0,0,0.1); }
        h1 { color: #0d6efd; margin-bottom: 30px; }
        .token-group { display: none; }
    </style>
</head>
<body>
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="form-container">
                    <h1 class="text-center">Enviar Notificación Push</h1>
                    
                    <form method="post" action="env_mensaje.php">
                        <div class="mb-3">
                            <label for="titulo" class="form-label">Título de la notificación</label>
                            <input type="text" class="form-control" id="titulo" name="titulo" required>
                        </div>
                        
                        <div class="mb-3">
                            <label for="mensaje" class="form-label">Mensaje</label>
                            <textarea class="form-control" id="mensaje" name="mensaje" rows="3" required></textarea>
                        </div>
                        
                        <div class="mb-3" hidden>
                            <label class="form-label">Destino</label>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="destino" id="destino_individual" value="individual">
                                <label class="form-check-label" for="destino_individual">
                                    Dispositivo individual
                                </label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="destino" id="destino_todos" value="todos" checked>
                                <label class="form-check-label" for="destino_todos">
                                    Todos los dispositivos
                                </label>
                            </div>
                        </div>
                        
                        <div class="mb-3 token-group" id="token_group">
                            <label for="token_individual" class="form-label">Token del dispositivo</label>
                            <select name="token_individual">
                                
                            </select>
                        </div>
                        
                        <div class="d-grid gap-2">
                            <button type="submit" class="btn btn-primary">Enviar Notificación</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Mostrar/ocultar campo de token según selección
        document.addEventListener('DOMContentLoaded', function() {
            const destinoIndividual = document.getElementById('destino_individual');
            const tokenGroup = document.getElementById('token_group');
            
            function toggleTokenField() {
                if (destinoIndividual.checked) {
                    tokenGroup.style.display = 'block';
                } else {
                    tokenGroup.style.display = 'none';
                }
            }
            
            // Escuchar cambios en los radio buttons
            document.querySelectorAll('input[name="destino"]').forEach(radio => {
                radio.addEventListener('change', toggleTokenField);
            });
            
            // Inicializar estado
            toggleTokenField();
        });
    </script>
</body>
</html>
