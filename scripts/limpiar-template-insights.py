#!/usr/bin/env python3
"""
Limpia los datos de un template Excel de Insights sin romper las referencias
a tablas dinámicas, fórmulas ni el Data Model.

Elimina todas las filas de datos (preservando headers) de las hojas:
  - FACT
  - Total Empresa
  - Calendario

Ajusta los rangos de las tablas Excel (FACT, Total_Empresa, Calendario)
para que apunten solo al header + 1 fila vacía (rango mínimo).

Requiere: lxml (pip install lxml)

Uso:
  python3 limpiar-template-insights.py <template.xlsx> [--output <salida.xlsx>]

Si no se especifica --output, se genera con sufijo _limpio.
"""

import argparse
import os
import sys
import zipfile
from lxml import etree

# Namespace de SpreadsheetML
NS = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'
NS_R = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'
NS_PKG = 'http://schemas.openxmlformats.org/package/2006/relationships'

# Hojas a limpiar y sus tablas con la última columna
SHEETS_TO_CLEAN = {
    'FACT':           {'table_name': 'FACT',           'last_col': 'Q'},
    'Total Empresa':  {'table_name': 'Total_Empresa',  'last_col': 'O'},
    'Calendario':     {'table_name': 'Calendario',     'last_col': 'D'},
}


def parse_args():
    parser = argparse.ArgumentParser(description='Limpia datos de un template Insights Excel')
    parser.add_argument('input', help='Archivo template .xlsx de entrada')
    parser.add_argument('--output', '-o', help='Archivo .xlsx de salida (default: <input>_limpio.xlsx)')
    return parser.parse_args()


def get_sheet_file_map(z):
    """Retorna {sheet_name: xl/worksheets/sheetN.xml}"""
    wb = etree.parse(z.open('xl/workbook.xml'))
    rels = etree.parse(z.open('xl/_rels/workbook.xml.rels'))

    rid_to_file = {}
    for r in rels.iter(f'{{{NS_PKG}}}Relationship'):
        rid_to_file[r.get('Id')] = r.get('Target')

    sheet_map = {}
    for sh in wb.iter(f'{{{NS}}}sheet'):
        name = sh.get('name')
        rid = sh.get(f'{{{NS_R}}}id')
        target = rid_to_file.get(rid)
        if target:
            sheet_map[name] = 'xl/' + target
    return sheet_map


def clean_sheet_data(xml_bytes):
    """
    Elimina todas las filas excepto la primera (header) de una hoja.
    Retorna los bytes XML modificados y la cantidad de filas eliminadas.
    lxml preserva los namespaces originales del documento.
    """
    root = etree.fromstring(xml_bytes)

    sheet_data = root.find(f'{{{NS}}}sheetData')
    if sheet_data is None:
        return xml_bytes, 0

    rows = sheet_data.findall(f'{{{NS}}}row')
    if len(rows) <= 1:
        return xml_bytes, 0

    removed = 0
    for row in rows[1:]:  # Preservar fila 0 (header en r="1")
        sheet_data.remove(row)
        removed += 1

    # Actualizar dimension si existe
    dim = root.find(f'{{{NS}}}dimension')
    if dim is not None:
        header_cells = rows[0].findall(f'{{{NS}}}c') if rows else []
        if header_cells:
            last_ref = header_cells[-1].get('r', 'A1')
            col_part = ''.join(c for c in last_ref if c.isalpha())
            dim.set('ref', f'A1:{col_part}1')

    return etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True), removed


def update_table_ref(xml_bytes, last_col):
    """
    Ajusta el rango de una tabla Excel a solo header + 1 fila (A1:{last_col}2).
    Esto es el mínimo para que la tabla sea válida. El código Java la redimensiona
    al generar el reporte.
    """
    root = etree.fromstring(xml_bytes)
    old_ref = root.get('ref', '?')
    new_ref = f'A1:{last_col}2'
    root.set('ref', new_ref)

    # Actualizar autoFilter si existe
    auto_filter = root.find(f'{{{NS}}}autoFilter')
    if auto_filter is not None:
        auto_filter.set('ref', new_ref)

    return etree.tostring(root, xml_declaration=True, encoding='UTF-8', standalone=True), old_ref, new_ref


def find_table_files(z):
    """Retorna {table_name: path_in_zip}"""
    table_map = {}
    for name in z.namelist():
        if '/tables/table' in name and name.endswith('.xml'):
            root = etree.fromstring(z.read(name))
            tname = root.get('name', '')
            table_map[tname] = name
    return table_map


def main():
    args = parse_args()

    if not os.path.exists(args.input):
        print(f'Error: no existe el archivo {args.input}', file=sys.stderr)
        sys.exit(1)

    if args.output:
        output = args.output
    else:
        base, ext = os.path.splitext(args.input)
        output = f'{base}_limpio{ext}'

    print(f'Entrada:  {args.input}')
    print(f'Salida:   {output}')
    print()

    with zipfile.ZipFile(args.input, 'r') as zin:
        sheet_map = get_sheet_file_map(zin)
        table_map = find_table_files(zin)

        # Recopilar archivos a modificar
        modifications = {}

        # 1. Limpiar hojas de datos
        for sheet_name, config in SHEETS_TO_CLEAN.items():
            sheet_file = sheet_map.get(sheet_name)
            if not sheet_file:
                print(f'  WARN: Hoja "{sheet_name}" no encontrada, se omite.')
                continue

            xml_bytes = zin.read(sheet_file)
            cleaned, removed = clean_sheet_data(xml_bytes)
            modifications[sheet_file] = cleaned
            print(f'  Hoja "{sheet_name}": eliminadas {removed} filas de datos')

        # 2. Ajustar rangos de tablas
        for sheet_name, config in SHEETS_TO_CLEAN.items():
            tname = config['table_name']
            last_col = config['last_col']
            table_file = table_map.get(tname)
            if not table_file:
                print(f'  WARN: Tabla "{tname}" no encontrada, se omite.')
                continue

            xml_bytes = zin.read(table_file)
            updated, old_ref, new_ref = update_table_ref(xml_bytes, last_col)
            modifications[table_file] = updated
            print(f'  Tabla "{tname}": {old_ref} -> {new_ref}')

    # 3. Reescribir el ZIP con las modificaciones
    temp_output = output + '.tmp'
    with zipfile.ZipFile(args.input, 'r') as src, \
         zipfile.ZipFile(temp_output, 'w', zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            if item.filename in modifications:
                dst.writestr(item, modifications[item.filename])
            else:
                dst.writestr(item, src.read(item.filename))

    os.replace(temp_output, output)

    # Verificar resultado
    print()
    with zipfile.ZipFile(output, 'r') as z:
        input_size = os.path.getsize(args.input)
        output_size = os.path.getsize(output)
        print(f'Tamano original:  {input_size / 1024 / 1024:.1f} MB')
        print(f'Tamano limpio:    {output_size / 1024 / 1024:.1f} MB')
        print(f'Reduccion:        {(input_size - output_size) / 1024 / 1024:.1f} MB ({(1 - output_size/input_size)*100:.0f}%)')

        # Verificar headers preservados
        print('\nVerificacion de headers:')
        sheet_map_out = get_sheet_file_map(z)
        for sheet_name in SHEETS_TO_CLEAN:
            sheet_file = sheet_map_out.get(sheet_name)
            if not sheet_file:
                continue
            root = etree.fromstring(z.read(sheet_file))
            sheet_data = root.find(f'{{{NS}}}sheetData')
            rows = sheet_data.findall(f'{{{NS}}}row') if sheet_data is not None else []
            if rows:
                cells = rows[0].findall(f'{{{NS}}}c')
                print(f'  {sheet_name}: {len(rows)} fila(s), {len(cells)} columnas en header - OK')
            else:
                print(f'  {sheet_name}: SIN FILAS - ERROR!')

        # Verificar hojas de presentacion
        presentation_sheets = [s for s in sheet_map_out if s not in SHEETS_TO_CLEAN and s != 'DIM']
        print(f'\nHojas de presentacion intactas: {len(presentation_sheets)}')
        for s in presentation_sheets:
            print(f'  - {s}')

        # Verificar pivot tables
        pt_count = sum(1 for n in z.namelist() if 'pivotTable' in n and n.endswith('.xml'))
        pc_count = sum(1 for n in z.namelist() if 'pivotCacheDefinition' in n)
        print(f'\nTablas dinamicas: {pt_count} pivot tables, {pc_count} pivot caches (sin modificar)')

    print('\nListo. El template limpio esta listo para usarse.')


if __name__ == '__main__':
    main()
