<?php
header('Content-Type: application/json; charset=utf-8');

// Conexión a la base de datos
$servername = "localhost";
$username = "Xalarrazabal025"; 
$password = "vRN7UMCCFV"; 
$dbname = "Xalarrazabal025_unigo"; 

$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar la conexión
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Error de conexión"]));
}

$conn->set_charset("utf8mb4");

// Verificar si se recibieron datos por POST
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $mail = $_POST['mail'] ?? '';
    $password = $_POST['password'] ?? '';

    if (empty($username) || empty($password)) {
        echo json_encode(["status" => "error", "message" => "Campos vacíos"]);
        exit();
    }

    // Consultar usuario
    $stmt = $conn->prepare("SELECT id, nombre, apellido, mail, contra, foto FROM Xalarrazabal025_usuarios WHERE mail = ?");
    $stmt->bind_param("s", $mail);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $userId = $row['id'];
        $nombre = $row['nombre'];
        $apellido = $row['apellido'];
        $mail = $row['mail'];
        $hashedPassword = $row['contra'];

        // Verificar contraseña (asumiendo MD5)
        if (md5($password) === $hashedPassword) {
            // Generar token
            $token = bin2hex(random_bytes(32));

            // Insertar token (fecha se autogenera)
            $stmt_token = $conn->prepare("INSERT INTO Xalarrazabal025_user_app (idUser, token) VALUES (?, ?)");
            $stmt_token->bind_param("is", $userId, $token);

            if ($stmt_token->execute()) {
                echo json_encode([
                    "status" => "success",
                    "token" => $token,
                    "mensaje" => "Inicio de sesión exitoso",
                    "nombre" => $nombre,
                    "apellido" => $apellido,
                    "mail" => $mail
                ]);
            } else {
                echo json_encode([
                    "status" => "error",
                    "message" => "Error al guardar el token: " . $conn->error
                ]);
            }

            $stmt_token->close();
        } else {
            echo json_encode([
                "status" => "error",
                "message" => "Contraseña incorrecta"
            ]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Usuario no encontrado"]);
    }

    $stmt->close();
} else {
    echo json_encode(["status" => "error", "message" => "Método no permitido"]);
}

$conn->close();
?>

