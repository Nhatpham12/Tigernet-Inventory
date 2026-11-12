"""Sinh file Word từ Markdown cho phần bổ sung SRS v1.2 và báo cáo đánh giá.

Chạy từ thư mục gốc repo:
    python3 docs/tools/build_docx_bo_sung.py

- docs/SRS_He_Thong_Quan_Ly_Kho_v1_2.docx = SRS v1.1 + chương 9 (từ docs/SRS_v1_2_Bo_sung.md),
  chèn trước "PHỤ LỤC", cập nhật bảng phiên bản.
- docs/Bao_cao_danh_gia_phuong_an_bo_sung.docx = báo cáo từ docs/Bao_cao_danh_gia_phuong_an_bo_sung.md.

Chỉ hỗ trợ phần Markdown mà hai file trên dùng: tiêu đề #/##/###, đoạn văn, **đậm**, `mã`,
danh sách "- " và "1. " (thụt 2-3 dấu cách = cấp 2), bảng |...|, khung "> ", ảnh ![chú thích](đường dẫn), "---".
"""
import copy
import os
import re

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

DOCS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
FONT = "Times New Roman"
BODY_PT = 13


def _shade(cell, hex_fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_fill)
    tc_pr.append(shd)


def _borders(table, color="000000"):
    tbl_pr = table._tbl.tblPr
    b = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        e = OxmlElement(f"w:{edge}")
        e.set(qn("w:val"), "single")
        e.set(qn("w:sz"), "4")
        e.set(qn("w:space"), "0")
        e.set(qn("w:color"), color)
        b.append(e)
    tbl_pr.append(b)


def _repeat_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    h = OxmlElement("w:tblHeader")
    h.set(qn("w:val"), "true")
    tr_pr.append(h)


INLINE = re.compile(r"(\*\*[^*]+\*\*|`[^`]+`)")


def add_runs(par, text, size=BODY_PT, bold=False, italic=False, color=None):
    for part in INLINE.split(text):
        if not part:
            continue
        if part.startswith("**") and part.endswith("**"):
            r = par.add_run(part[2:-2])
            r.bold = True
        elif part.startswith("`") and part.endswith("`"):
            r = par.add_run(part[1:-1])
            r.font.name = "Consolas"
            r._element.rPr.rFonts.set(qn("w:eastAsia"), "Consolas")
            r.font.size = Pt(size - 1.5)
            r.font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
            continue
        else:
            r = par.add_run(part)
            if bold:
                r.bold = True
        r.font.name = FONT
        r.font.size = Pt(size)
        if italic:
            r.italic = True
        if color:
            r.font.color.rgb = color


class Builder:
    """Ghi các khối vào tài liệu; nếu có anchor thì chèn trước anchor."""

    def __init__(self, doc, anchor=None, heading_shift=0, base_dir=DOCS, bullet_proto=None):
        self.doc = doc
        self.anchor = anchor
        self.shift = heading_shift
        self.base = base_dir
        self.bullet_proto = bullet_proto
        self.text_width = doc.sections[0].page_width - doc.sections[0].left_margin - doc.sections[0].right_margin

    def _place(self, element):
        if self.anchor is not None:
            self.anchor._p.addprevious(element)
        else:
            body = self.doc.element.body
            body.insert(len(body) - 1, element)  # trước sectPr

    def para(self, style=None):
        p = self.doc.add_paragraph(style=style)
        self._place(p._p)
        return p

    def heading(self, text, level):
        level = min(max(level + self.shift, 1), 3)
        p = self.para(style=f"Heading {level}")
        add_runs(p, text, size={1: 16, 2: 14, 3: 13}[level], bold=True)
        return p

    def text(self, text):
        p = self.para()
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        add_runs(p, text)

    def bullet(self, text, level=0, number=None):
        p = self.para()
        pf = p.paragraph_format
        pf.left_indent = Cm(1.0 + 0.8 * level)
        pf.first_line_indent = Cm(-0.6)
        pf.space_after = Pt(3)
        marker = f"{number}." if number else ("•" if level == 0 else "–")
        add_runs(p, f"{marker}\t{text}")
        pf.tab_stops.add_tab_stop(Cm(1.0 + 0.8 * level))

    def callout(self, text):
        t = self.doc.add_table(rows=1, cols=1)
        self._place(t._tbl)
        _borders(t, "D6B656")
        c = t.cell(0, 0)
        c.width = self.text_width
        t.columns[0].width = self.text_width
        tr_pr = t.rows[0]._tr.get_or_add_trPr()
        cs = OxmlElement("w:cantSplit")
        cs.set(qn("w:val"), "true")
        tr_pr.append(cs)
        _shade(c, "FFF2CC")
        p = c.paragraphs[0]
        p.paragraph_format.space_before = Pt(3)
        p.paragraph_format.space_after = Pt(3)
        add_runs(p, text, size=BODY_PT)
        self.para()  # khoảng trống sau khung

    def table(self, rows):
        header, body = rows[0], rows[1:]
        t = self.doc.add_table(rows=1 + len(body), cols=len(header))
        self._place(t._tbl)
        t.alignment = WD_TABLE_ALIGNMENT.CENTER
        _borders(t)
        size = 11 if len(header) >= 5 else 12
        for i, row in enumerate([header] + body):
            for j in range(len(header)):
                val = row[j] if j < len(row) else ""
                cell = t.cell(i, j)
                p = cell.paragraphs[0]
                p.paragraph_format.space_after = Pt(1)
                add_runs(p, val, size=size, bold=(i == 0))
                if i == 0:
                    _shade(cell, "D9E2F3")
        _repeat_header(t.rows[0])
        # Độ rộng cột: mỗi cột tối thiểu đủ cho từ dài nhất, phần còn lại chia theo độ dài nội dung
        char_cm = 0.2 if size >= 12 else 0.185
        mins, weights = [], []
        for j in range(len(header)):
            cells = [r[j].replace("`", "").replace("**", "") if j < len(r) else "" for r in [header] + body]
            longest = max([len(w) for c in cells for w in c.split()] + [2])
            mins.append(Cm(longest * char_cm + 0.45))
            avg = sum(len(c) for c in cells) / len(cells)
            weights.append(max(avg, 1.0))
        spare = max(self.text_width - sum(mins), 0)
        total_w = sum(weights)
        t.autofit = False
        for j in range(len(header)):
            w = int(mins[j] + spare * weights[j] / total_w)
            t.columns[j].width = w  # gridCol, LibreOffice đọc giá trị này
            for row in t.rows:
                row.cells[j].width = w
        self.para()

    def image(self, caption, path):
        full = os.path.normpath(os.path.join(self.base, path))
        from PIL import Image

        w, h = Image.open(full).size
        max_w = self.text_width
        max_h = Cm(22)
        width = min(max_w, int(max_h * w / h))
        p = self.para()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run().add_picture(full, width=width)
        c = self.para()
        c.alignment = WD_ALIGN_PARAGRAPH.CENTER
        add_runs(c, caption, size=11, italic=True)

    def render(self, md, skip_until=None):
        lines = md.splitlines()
        i = 0
        if skip_until:
            while i < len(lines) and not lines[i].startswith(skip_until):
                i += 1
        while i < len(lines):
            line = lines[i].rstrip()
            s = line.strip()
            if not s or s == "---":
                i += 1
                continue
            m = re.match(r"^(#{1,3}) (.*)$", line)
            if m:
                self.heading(m.group(2), len(m.group(1)))
                i += 1
                continue
            if s.startswith("|"):
                rows = []
                while i < len(lines) and lines[i].strip().startswith("|"):
                    cells = [c.strip() for c in lines[i].strip().strip("|").split(" | ")]
                    if not all(re.fullmatch(r":?-{3,}:?", c) for c in cells):
                        rows.append(cells)
                    i += 1
                self.table(rows)
                continue
            if s.startswith(">"):
                buf = []
                while i < len(lines) and lines[i].strip().startswith(">"):
                    buf.append(lines[i].strip().lstrip(">").strip())
                    i += 1
                self.callout(" ".join(buf))
                continue
            m = re.match(r"^!\[(.*)\]\((.*)\)$", s)
            if m:
                self.image(m.group(1), m.group(2))
                i += 1
                continue
            m = re.match(r"^( *)- (.*)$", line)
            if m:
                self.bullet(m.group(2), level=1 if len(m.group(1)) >= 2 else 0)
                i += 1
                continue
            m = re.match(r"^( *)(\d+)\. (.*)$", line)
            if m:
                self.bullet(m.group(3), level=1 if len(m.group(1)) >= 2 else 0, number=m.group(2))
                i += 1
                continue
            self.text(s)
            i += 1


def _set_cell_text(cell, text):
    p = cell.paragraphs[0]
    runs = p.runs
    runs[0].text = text
    for r in runs[1:]:
        r.text = ""


def build_srs():
    src = os.path.join(DOCS, "SRS_He_Thong_Quan_Ly_Kho_v1_1.docx")
    out = os.path.join(DOCS, "SRS_He_Thong_Quan_Ly_Kho_v1_2.docx")
    md = open(os.path.join(DOCS, "SRS_v1_2_Bo_sung.md"), encoding="utf-8").read()
    doc = Document(src)

    # Bảng thông tin phiên bản
    info = doc.tables[0]
    for row in info.rows:
        k = row.cells[0].text.strip()
        if k == "Phiên bản":
            _set_cell_text(row.cells[1], "1.2")
        elif k == "Ngày cập nhật":
            _set_cell_text(row.cells[1], "Tháng 11 năm 2026")

    for sec in doc.sections:
        for hp in sec.header.paragraphs:
            for r in hp.runs:
                r.text = r.text.replace("v1.1", "v1.2")

    paras = doc.paragraphs
    # Ghi chú phiên bản 1.2 ngay sau đoạn giới thiệu bản 1.1
    v11 = next(p for p in paras if p.text.startswith("Phiên bản 1.1 bổ sung"))
    new = copy.deepcopy(v11._p)
    v11._p.addnext(new)
    from docx.text.paragraph import Paragraph

    np_ = Paragraph(new, v11._parent)
    for r in np_.runs[1:]:
        r.text = ""
    np_.runs[0].text = (
        "Phiên bản 1.2 bổ sung chương 9: xem với vai trò khác (Act-as), báo lỗi cho phòng IT, "
        "phiếu lương có mã PIN, chốt đơn và giao lại việc, hồ sơ nhân viên và chấm công online, "
        "đăng nhập bằng tài khoản Microsoft của tổ chức, xử lý khách bom hàng, CRM đầy đủ và quy trình "
        "thiết kế lấy người dùng làm trung tâm (UCD). Lý do bổ sung được trình bày ngay đầu chương 9 "
        "và ở đầu từng mục."
    )

    # Mục 2.3: thêm dòng dẫn sang chương 9 sau chức năng 15
    f15 = next(p for p in paras if p.text.startswith("Tồn kho, cảnh báo và báo cáo theo từng kho"))
    add = copy.deepcopy(f15._p)
    f15._p.addnext(add)
    ap = Paragraph(add, f15._parent)
    for r in ap.runs[1:]:
        r.text = ""
    ap.runs[0].text = (
        "Bổ sung ở bản 1.2 (chi tiết chương 9): xem với vai trò khác; báo lỗi cho IT; phiếu lương có PIN; "
        "chốt đơn và giao lại việc; chấm công online; đăng nhập Microsoft; xử lý khách bom hàng; CRM đầy đủ."
    )
    if ap.runs[0].bold:
        ap.runs[0].bold = False

    anchor = next(p for p in doc.paragraphs if p.style.name == "Heading 1" and p.text.strip() == "PHỤ LỤC")
    b = Builder(doc, anchor=anchor, heading_shift=0)
    h1 = b.para(style="Heading 1")
    add_runs(h1, "9. YÊU CẦU BỔ SUNG (PHIÊN BẢN 1.2)", size=16, bold=True)
    b.render(md, skip_until="## 9.0")
    # Ngắt trang trước PHỤ LỤC
    br = b.para()
    from docx.enum.text import WD_BREAK

    br.add_run().add_break(WD_BREAK.PAGE)
    doc.core_properties.title = "SRS TigerNet v1.2"
    doc.save(out)
    return out


def build_report():
    out = os.path.join(DOCS, "Bao_cao_danh_gia_phuong_an_bo_sung.docx")
    md = open(os.path.join(DOCS, "Bao_cao_danh_gia_phuong_an_bo_sung.md"), encoding="utf-8").read()
    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21), Cm(29.7)
    sec.left_margin, sec.right_margin = Cm(2.5), Cm(2)
    sec.top_margin, sec.bottom_margin = Cm(2), Cm(2)
    st = doc.styles["Normal"]
    st.font.name = FONT
    st.font.size = Pt(BODY_PT)
    st.element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
    for lvl in (1, 2, 3):
        hs = doc.styles[f"Heading {lvl}"]
        hs.font.name = FONT
        hs.font.color.rgb = RGBColor(0x1F, 0x3A, 0x5F)
        hs.element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
        hs.element.rPr.rFonts.set(qn("w:ascii"), FONT)
        hs.element.rPr.rFonts.set(qn("w:hAnsi"), FONT)

    lines = md.splitlines()
    title = lines[0].lstrip("# ").strip()
    tp = doc.add_paragraph()
    tp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_runs(tp, title, size=18, bold=True, color=RGBColor(0x1F, 0x3A, 0x5F))
    b = Builder(doc, heading_shift=-1)
    b.render("\n".join(lines[1:]))

    # Số trang ở chân trang
    fp = sec.footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = fp.add_run()
    for tag, txt in (("begin", None), (None, "PAGE"), ("end", None)):
        if tag:
            fc = OxmlElement("w:fldChar")
            fc.set(qn("w:fldCharType"), tag)
            r._r.append(fc)
        else:
            it = OxmlElement("w:instrText")
            it.set(qn("xml:space"), "preserve")
            it.text = txt
            r._r.append(it)
    doc.core_properties.title = title
    doc.save(out)
    return out


if __name__ == "__main__":
    print(build_srs())
    print(build_report())
