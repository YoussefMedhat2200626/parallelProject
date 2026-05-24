"""
Convert the documentation Markdown to a professional PDF using fpdf2.
Handles headings, paragraphs, code blocks, tables, bold/italic, and lists.
"""
import re
import os
from fpdf import FPDF, XPos, YPos
import os
dir_path = os.path.dirname(os.path.realpath(__file__))
INPUT_MD = os.path.join(dir_path, "Distributed_Marketplace_Full_Documentation.md")
OUTPUT_PDF = os.path.join(dir_path, "Distributed_Marketplace_Full_Documentation.pdf")


class DocPDF(FPDF):
    def __init__(self):
        super().__init__(orientation='P', unit='mm', format='A4')
        self.set_auto_page_break(auto=True, margin=25)

        # Add Unicode fonts
        self.add_font("Inter", "", os.path.join(os.environ.get("LOCALAPPDATA", ""), "Microsoft", "Windows", "Fonts", "Inter-Regular.ttf"), uni=True) if os.path.exists(os.path.join(os.environ.get("LOCALAPPDATA", ""), "Microsoft", "Windows", "Fonts", "Inter-Regular.ttf")) else None
        self.add_font("Inter", "B", os.path.join(os.environ.get("LOCALAPPDATA", ""), "Microsoft", "Windows", "Fonts", "Inter-Bold.ttf"), uni=True) if os.path.exists(os.path.join(os.environ.get("LOCALAPPDATA", ""), "Microsoft", "Windows", "Fonts", "Inter-Bold.ttf")) else None

    def header(self):
        if self.page_no() > 1:
            self.set_font("Helvetica", "I", 8)
            self.set_text_color(150, 150, 150)
            self.cell(0, 5, "Distributed Online Marketplace System - CSE352s", new_x=XPos.LMARGIN, new_y=YPos.NEXT, align="C")
            self.line(15, 12, self.w - 15, 12)
            self.ln(3)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 10, f"Page {self.page_no()}/{{nb}}", align="C")


def sanitize_text(text):
    """Replace Unicode characters with latin-1 safe equivalents."""
    replacements = {
        '\u2014': '--',   # em-dash
        '\u2013': '-',    # en-dash
        '\u2018': "'",    # left single quote
        '\u2019': "'",    # right single quote
        '\u201c': '"',    # left double quote
        '\u201d': '"',    # right double quote
        '\u2026': '...',  # ellipsis
        '\u2022': '-',    # bullet
        '\u2192': '->',   # right arrow
        '\u2190': '<-',   # left arrow
        '\u2194': '<->',  # left-right arrow
        '\u2500': '-',    # box drawing
        '\u2502': '|',    # box drawing
        '\u250c': '+',    # box drawing
        '\u2510': '+',    # box drawing
        '\u2514': '+',    # box drawing
        '\u2518': '+',    # box drawing
        '\u251c': '+',    # box drawing
        '\u2524': '+',    # box drawing
        '\u252c': '+',    # box drawing
        '\u2534': '+',    # box drawing
        '\u253c': '+',    # box drawing
        '\u2550': '=',    # box drawing double
        '\u00d7': 'x',    # multiplication sign
        '\u2265': '>=',   # greater than or equal
        '\u2264': '<=',   # less than or equal
        '\u2260': '!=',   # not equal
        '\u00a0': ' ',    # non-breaking space
        '\u200b': '',     # zero width space
        '\ufeff': '',     # BOM
    }
    for char, replacement in replacements.items():
        text = text.replace(char, replacement)
    # Fallback: replace any remaining non-latin-1 characters
    try:
        text.encode('latin-1')
    except UnicodeEncodeError:
        text = text.encode('latin-1', errors='replace').decode('latin-1')
    return text


def clean_md_formatting(text):
    """Remove markdown inline formatting for plain text extraction."""
    text = re.sub(r'\*\*(.+?)\*\*', r'\1', text)
    text = re.sub(r'\*(.+?)\*', r'\1', text)
    text = re.sub(r'`(.+?)`', r'\1', text)
    text = re.sub(r'\[(.+?)\]\(.+?\)', r'\1', text)  # links
    return sanitize_text(text.strip())


def parse_table(lines):
    """Parse a markdown table into header and rows."""
    rows = []
    for line in lines:
        line = line.strip()
        if line.startswith('|') and line.endswith('|'):
            cells = [c.strip() for c in line[1:-1].split('|')]
            # Skip separator lines
            if all(re.match(r'^[-:]+$', c) for c in cells):
                continue
            rows.append(cells)
    if len(rows) < 1:
        return None, None
    return rows[0], rows[1:]


def build_pdf(md_path, pdf_path):
    pdf = DocPDF()
    pdf.alias_nb_pages()
    pdf.add_page()

    with open(md_path, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    i = 0
    in_code_block = False
    code_lines = []
    code_lang = ""
    in_table = False
    table_lines = []

    while i < len(lines):
        line = lines[i]

        # --- Code block start/end ---
        if line.strip().startswith('```'):
            if in_code_block:
                # End of code block - render it
                render_code_block(pdf, code_lines, code_lang)
                code_lines = []
                code_lang = ""
                in_code_block = False
            else:
                # Flush table if pending
                if in_table:
                    render_table(pdf, table_lines)
                    table_lines = []
                    in_table = False
                in_code_block = True
                code_lang = line.strip()[3:].strip()
            i += 1
            continue

        if in_code_block:
            code_lines.append(line)
            i += 1
            continue

        # --- Table detection ---
        if line.strip().startswith('|') and '|' in line.strip()[1:]:
            if not in_table:
                in_table = True
                table_lines = []
            table_lines.append(line)
            i += 1
            continue
        else:
            if in_table:
                render_table(pdf, table_lines)
                table_lines = []
                in_table = False

        # --- Horizontal rule ---
        if re.match(r'^---+\s*$', line.strip()):
            pdf.ln(3)
            y = pdf.get_y()
            pdf.set_draw_color(15, 52, 96)
            pdf.line(15, y, pdf.w - 15, y)
            pdf.ln(5)
            i += 1
            continue

        # --- Headings ---
        heading_match = re.match(r'^(#{1,6})\s+(.+)$', line)
        if heading_match:
            level = len(heading_match.group(1))
            text = clean_md_formatting(heading_match.group(2))
            render_heading(pdf, text, level)
            i += 1
            continue

        # --- Empty line ---
        if not line.strip():
            pdf.ln(2)
            i += 1
            continue

        # --- Blockquote ---
        if line.strip().startswith('>'):
            quote_text = line.strip()[1:].strip()
            # Handle multi-line blockquotes
            while i + 1 < len(lines) and lines[i + 1].strip().startswith('>'):
                i += 1
                quote_text += " " + lines[i].strip()[1:].strip()
            render_blockquote(pdf, clean_md_formatting(quote_text))
            i += 1
            continue

        # --- Unordered list ---
        if re.match(r'^\s*[-*]\s+', line):
            indent = len(line) - len(line.lstrip())
            text = re.sub(r'^\s*[-*]\s+', '', line)
            render_list_item(pdf, clean_md_formatting(text), indent)
            i += 1
            continue

        # --- Ordered list ---
        ol_match = re.match(r'^\s*(\d+)\.\s+(.+)', line)
        if ol_match:
            indent = len(line) - len(line.lstrip())
            num = ol_match.group(1)
            text = ol_match.group(2)
            render_list_item(pdf, clean_md_formatting(text), indent, number=num)
            i += 1
            continue

        # --- Center-aligned div (cover page) ---
        if '<div align="center">' in line:
            i += 1
            continue
        if '</div>' in line:
            i += 1
            continue

        # --- Regular paragraph ---
        para_text = line.strip()
        # Collect multi-line paragraph
        while i + 1 < len(lines) and lines[i + 1].strip() and not lines[i + 1].strip().startswith('#') and not lines[i + 1].strip().startswith('|') and not lines[i + 1].strip().startswith('```') and not lines[i + 1].strip().startswith('>') and not re.match(r'^\s*[-*]\s+', lines[i + 1]) and not re.match(r'^\s*\d+\.\s+', lines[i + 1]) and not re.match(r'^---+\s*$', lines[i + 1].strip()):
            i += 1
            para_text += " " + lines[i].strip()

        if para_text:
            render_paragraph(pdf, clean_md_formatting(para_text))

        i += 1

    # Flush remaining table
    if in_table:
        render_table(pdf, table_lines)

    pdf.output(pdf_path)
    print(f"PDF generated: {pdf_path}")


def render_heading(pdf, text, level):
    sizes = {1: 20, 2: 16, 3: 13, 4: 11.5, 5: 10.5, 6: 10}
    size = sizes.get(level, 10)

    if level <= 2:
        pdf.ln(6)

    pdf.set_font("Helvetica", "B", size)
    pdf.set_text_color(15, 52, 96)
    pdf.multi_cell(0, size * 0.5, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    if level == 1:
        y = pdf.get_y()
        pdf.set_draw_color(15, 52, 96)
        pdf.set_line_width(0.8)
        pdf.line(15, y, pdf.w - 15, y)
        pdf.set_line_width(0.2)
        pdf.ln(4)
    elif level == 2:
        y = pdf.get_y()
        pdf.set_draw_color(200, 200, 200)
        pdf.set_line_width(0.4)
        pdf.line(15, y, pdf.w - 15, y)
        pdf.set_line_width(0.2)
        pdf.ln(3)
    else:
        pdf.ln(2)


def render_paragraph(pdf, text):
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(30, 30, 46)
    pdf.multi_cell(0, 5, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(2)


def render_code_block(pdf, code_lines, lang=""):
    pdf.ln(2)
    # Background box
    x = pdf.get_x()
    y = pdf.get_y()

    code_text = "\n".join(code_lines)

    pdf.set_fill_color(30, 30, 46)
    pdf.set_text_color(205, 214, 244)
    pdf.set_font("Courier", "", 7.5)

    # Draw left accent bar
    pdf.set_draw_color(15, 52, 96)

    # Calculate height needed
    line_h = 3.5
    num_lines = len(code_lines)
    block_h = max(num_lines * line_h + 8, 12)

    # Check page break
    if pdf.get_y() + block_h > pdf.h - 25:
        pdf.add_page()
        y = pdf.get_y()

    # Draw background
    pdf.rect(17, y, pdf.w - 32, block_h, style='F')
    pdf.set_fill_color(15, 52, 96)
    pdf.rect(15, y, 2, block_h, style='F')

    # Label
    if lang and lang != "mermaid":
        pdf.set_font("Courier", "B", 6.5)
        pdf.set_xy(19, y + 1)
        pdf.set_text_color(100, 150, 220)
        pdf.cell(30, 3, lang.upper(), new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # Code text
    pdf.set_font("Courier", "", 7.5)
    pdf.set_text_color(205, 214, 244)
    start_y = y + (5 if lang else 3)
    pdf.set_xy(20, start_y)

    for cl in code_lines:
        if pdf.get_y() + line_h > y + block_h:
            break
        # Truncate very long lines
        if len(cl) > 110:
            cl = cl[:107] + "..."
        pdf.cell(0, line_h, sanitize_text(cl.replace('\t', '    ')), new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.set_x(20)

    pdf.set_y(y + block_h + 2)
    pdf.set_text_color(30, 30, 46)
    pdf.ln(2)


def render_table(pdf, table_lines):
    header, rows = parse_table(table_lines)
    if header is None:
        return

    num_cols = len(header)
    usable_w = pdf.w - 30  # margins
    col_w = usable_w / num_cols

    # Auto-adjust column widths based on content
    col_widths = []
    for j in range(num_cols):
        max_len = len(clean_md_formatting(header[j]))
        for row in (rows or []):
            if j < len(row):
                max_len = max(max_len, len(clean_md_formatting(row[j])))
        col_widths.append(max_len)

    total = sum(col_widths) if sum(col_widths) > 0 else 1
    col_widths = [(w / total) * usable_w for w in col_widths]
    # Ensure minimum width
    col_widths = [max(w, 15) for w in col_widths]
    # Re-normalize
    total_w = sum(col_widths)
    if total_w > usable_w:
        col_widths = [(w / total_w) * usable_w for w in col_widths]

    pdf.ln(2)

    # Check if table fits on page
    estimated_h = (len(rows or []) + 1) * 7 + 4
    if pdf.get_y() + estimated_h > pdf.h - 25 and estimated_h < pdf.h - 50:
        pdf.add_page()

    # Header row
    pdf.set_fill_color(15, 52, 96)
    pdf.set_text_color(255, 255, 255)
    pdf.set_font("Helvetica", "B", 7.5)
    for j, h in enumerate(header):
        w = col_widths[j] if j < len(col_widths) else col_w
        pdf.cell(w, 7, clean_md_formatting(h)[:40], border=1, fill=True)
    pdf.ln()

    # Data rows
    pdf.set_text_color(30, 30, 46)
    pdf.set_font("Helvetica", "", 7.5)
    for ri, row in enumerate(rows or []):
        # Alternating row colors
        if ri % 2 == 0:
            pdf.set_fill_color(248, 249, 250)
        else:
            pdf.set_fill_color(255, 255, 255)

        for j in range(num_cols):
            w = col_widths[j] if j < len(col_widths) else col_w
            cell_text = clean_md_formatting(row[j]) if j < len(row) else ""
            # Truncate long text
            if len(cell_text) > 60:
                cell_text = cell_text[:57] + "..."
            pdf.cell(w, 6, cell_text, border=1, fill=True)
        pdf.ln()

    pdf.ln(3)


def render_blockquote(pdf, text):
    pdf.ln(2)
    x = pdf.get_x()
    y = pdf.get_y()

    pdf.set_fill_color(240, 244, 255)
    pdf.set_draw_color(15, 52, 96)

    # Calculate height
    pdf.set_font("Helvetica", "I", 9)
    lines_needed = pdf.get_string_width(text) / (pdf.w - 40) + 1
    block_h = max(lines_needed * 5 + 6, 10)

    pdf.rect(17, y, pdf.w - 32, block_h, style='F')
    pdf.set_fill_color(15, 52, 96)
    pdf.rect(15, y, 2, block_h, style='F')

    pdf.set_text_color(50, 50, 80)
    pdf.set_xy(20, y + 3)
    pdf.multi_cell(pdf.w - 40, 5, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_y(y + block_h + 2)
    pdf.set_text_color(30, 30, 46)


def render_list_item(pdf, text, indent=0, number=None):
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(30, 30, 46)
    x_offset = 20 + (indent * 3)
    pdf.set_x(x_offset)
    bullet = f"{number}." if number else "-"
    pdf.cell(6, 5, bullet)
    pdf.multi_cell(pdf.w - x_offset - 20, 5, sanitize_text(text), new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(1)


if __name__ == "__main__":
    build_pdf(INPUT_MD, OUTPUT_PDF)
