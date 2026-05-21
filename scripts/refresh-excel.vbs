' scripts/refresh-excel.vbs
' Abre un archivo Excel con COM Automation, ejecuta RefreshAll para reconstruir
' el modelo de datos VertiPaq, y guarda el resultado.
' Requiere: Excel instalado en el servidor Windows.
'
' VBScript corre nativamente en STA (Single-Threaded Apartment),
' lo que evita los problemas de COM que tiene PowerShell desde servicios.
'
' Uso: cscript.exe //Nologo //B refresh-excel.vbs "C:\reportes\archivo.xlsx"

Dim filePath, fso, excel, workbook

If WScript.Arguments.Count < 1 Then
    WScript.StdErr.WriteLine "Uso: cscript.exe //Nologo //B refresh-excel.vbs <ruta-archivo.xlsx>"
    WScript.Quit 1
End If

filePath = WScript.Arguments(0)

Set fso = CreateObject("Scripting.FileSystemObject")
If Not fso.FileExists(filePath) Then
    WScript.StdErr.WriteLine "Archivo no encontrado: " & filePath
    WScript.Quit 1
End If

' Convertir a ruta absoluta
filePath = fso.GetAbsolutePathName(filePath)

On Error Resume Next

Set excel = CreateObject("Excel.Application")
If Err.Number <> 0 Then
    WScript.StdErr.WriteLine "No se pudo crear Excel.Application: " & Err.Description
    WScript.Quit 1
End If

excel.Visible = False
excel.DisplayAlerts = False
excel.AskToUpdateLinks = False
excel.EnableEvents = False

Set workbook = excel.Workbooks.Open(filePath, False, False)
If Err.Number <> 0 Then
    WScript.StdErr.WriteLine "No se pudo abrir el archivo: " & Err.Description
    excel.Quit
    Set excel = Nothing
    WScript.Quit 1
End If

Err.Clear

' Deshabilitar background queries para que RefreshAll sea sincrónico
On Error Resume Next
Dim conn
For Each conn In workbook.Connections
    If conn.Type = 1 Then  ' xlConnectionTypeOLEDB
        conn.OLEDBConnection.BackgroundQuery = False
    ElseIf conn.Type = 2 Then  ' xlConnectionTypeODBC
        conn.ODBCConnection.BackgroundQuery = False
    End If
    Err.Clear
Next

' RefreshAll: reconstruye el modelo de datos VertiPaq desde las tablas
workbook.RefreshAll
If Err.Number <> 0 Then
    WScript.StdErr.WriteLine "Error en RefreshAll: " & Err.Description
    Err.Clear
End If

' Esperar a que TODAS las queries async terminen (correcto para modelos grandes)
excel.CalculateUntilAsyncQueriesDone
Err.Clear

' Re-ocultar hojas de datos que POI marcó como hidden
' (Excel COM puede resetear la visibilidad al hacer RefreshAll/Save)
Dim sheetNames, i, ws
sheetNames = Array("FACT", "Calendario", "Total_Empresa", "DIM", "Hoja 1")
For Each i In sheetNames
    On Error Resume Next
    Set ws = workbook.Sheets(i)
    If Err.Number = 0 Then
        ws.Visible = 0  ' xlSheetHidden
    End If
    Err.Clear
Next

' Guardar
workbook.Save
If Err.Number <> 0 Then
    WScript.StdErr.WriteLine "Error al guardar: " & Err.Description
    workbook.Close False
    excel.Quit
    Set workbook = Nothing
    Set excel = Nothing
    WScript.Quit 1
End If

' Limpiar
workbook.Close False
excel.Quit
Set workbook = Nothing
Set excel = Nothing

WScript.StdOut.WriteLine "OK"
WScript.Quit 0
