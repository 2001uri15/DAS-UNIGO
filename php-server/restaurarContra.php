<?php
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'PHPMailer/src/Exception.php';
require 'PHPMailer/src/PHPMailer.php';
require 'PHPMailer/src/SMTP.php';

// Conexión a la base de datos
$servername = "localhost";
$username = "Xalarrazabal025"; 
$password = "vRN7UMCCFV"; 
$dbname = "Xalarrazabal025_unigo";

// Recibir el correo electrónico
$email = isset($_POST['email']) ? trim($_POST['email']) : '';

if(empty($email)) {
    die(json_encode(['status' => 'error', 'message' => 'No se ha proporcionado un correo electrónico.']));
}

try {
    // Crear conexión PDO
    $conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Verificar si el correo existe en la base de datos
    $stmt = $conn->prepare("SELECT * FROM app_user WHERE mail = :email");
    $stmt->bindParam(':email', $email);
    $stmt->execute();

    if($stmt->rowCount() == 0) {
        die(json_encode(['status' => 'error', 'message' => 'El correo electrónico no está registrado.']));
    }

    // Generar nueva contraseña de 8 dígitos
    $newPassword = substr(str_shuffle('0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'), 0, 8);
    $hashedPassword = md5($newPassword);

    // Configurar PHPMailer para usar SMTP de la EHU
    $mail = new PHPMailer(true);
    $mail->CharSet = 'UTF-8'; // Codificación explícita
    $mail->Encoding = 'base64'; // Para caracteres especiales
    $mail->isSMTP();
    $mail->Host = '#####'; // Servidor SMTP de la EHU
    $mail->SMTPAuth = true;
    $mail->Username = '#####'; // Tu usuario (sin @ehu.es puede que no funcione)
    $mail->Password = '#####'; // Tu contraseña
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS; // TLS obligatorio en la EHU
    $mail->Port = 587; // Puerto SMTP EHU

    // Remitente y destinatario
    $mail->setFrom('######', 'MovGasteiz'); // Usa tu correo EHU
    $mail->addAddress($email);
    $mail->addBCC('alarrazabal025@ikasle.ehu.eus');

    // Contenido del correo
    $mail->isHTML(false); // Correo en texto plano
    $mail->Subject = 'MovGasteiz | Cambio de contraseña';
    $mail->Body = "Estimado usuario,\n\n"
                . "Hemos recibido una solicitud para cambiar su contraseña.\n\n"
                . "Su nueva contraseña es: $newPassword\n\n"
                . "Un saludo,\n"
                . "Equipo de MovGasteiz";

    // Enviar el correo
    $mail->send();

    // Si el correo se envía, actualizar la contraseña en la BD
    $updateStmt = $conn->prepare("UPDATE app_user SET contra = :password WHERE mail = :email");
    $updateStmt->bindParam(':password', $hashedPassword);
    $updateStmt->bindParam(':email', $email);
    $updateStmt->execute();

    echo json_encode(['status' => 'success', 'message' => 'Se ha enviado un correo con su nueva contraseña.']);

} catch (Exception $e) {
    error_log("Error al enviar correo: " . $e->getMessage());
    die(json_encode(['status' => 'error', 'message' => 'Error al enviar el correo. Por favor, inténtelo más tarde.']));
}
?>
