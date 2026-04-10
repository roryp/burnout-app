"""Generate a single-slide PPTX with a horizontal 6-phase pipeline diagram."""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

BG = RGBColor(0x0F, 0x17, 0x2A)
WHITE = RGBColor(0xF8, 0xFA, 0xFC)
MUTED = RGBColor(0x94, 0xA3, 0xB8)
ARROW_CLR = RGBColor(0x47, 0x55, 0x69)

PHASES = [
    {"num": "1", "title": "Ingestion",              "color": RGBColor(0x3B, 0x82, 0xF6), "sub": "GitHub Issues\n→ Cache"},
    {"num": "2", "title": "Chaos Metrics",           "color": RGBColor(0xF5, 0x9E, 0x0B), "sub": "5 Signals\n→ Score 0–10"},
    {"num": "3", "title": "Classification",          "color": RGBColor(0xA7, 0x8B, 0xFA), "sub": "4 Buckets\n+ Compliance"},
    {"num": "4", "title": "Stress Score",            "color": RGBColor(0x2D, 0xD4, 0xBF), "sub": "WorldState\n→ 0–100"},
    {"num": "5", "title": "AI Agents",               "color": RGBColor(0xFB, 0x71, 0x85), "sub": "5 Sub-Agents\n+ Fallback"},
    {"num": "6", "title": "Output",                  "color": RGBColor(0x81, 0x8C, 0xF8), "sub": "Mutations\n+ Persistence"},
]


def set_bg(slide, color):
    fill = slide.background.fill
    fill.solid()
    fill.fore_color.rgb = color


def add_rounded_box(slide, left, top, width, height, fill_color):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill_color
    shape.line.fill.background()
    shape.shadow.inherit = False
    # Reduce corner rounding
    shape.adjustments[0] = 0.1
    return shape


def add_arrow(slide, left, top, width, height):
    shape = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = ARROW_CLR
    shape.line.fill.background()
    shape.shadow.inherit = False
    shape.rotation = 0.0
    return shape


def main():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    slide = prs.slides.add_slide(prs.slide_layouts[6])  # blank
    set_bg(slide, BG)

    # Title
    tx = slide.shapes.add_textbox(Inches(0), Inches(0.6), prs.slide_width, Inches(0.7))
    tf = tx.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = "Burnout-as-a-Service — Algorithm Pipeline"
    p.font.size = Pt(32)
    p.font.bold = True
    p.font.color.rgb = WHITE
    p.font.name = "Segoe UI"
    p.alignment = PP_ALIGN.CENTER

    # Subtitle
    tx2 = slide.shapes.add_textbox(Inches(0), Inches(1.25), prs.slide_width, Inches(0.4))
    tf2 = tx2.text_frame
    tf2.word_wrap = True
    p2 = tf2.paragraphs[0]
    p2.text = "Deterministic services compute all metrics  •  AI agents only explain & act  •  Every agent has a fallback"
    p2.font.size = Pt(14)
    p2.font.color.rgb = MUTED
    p2.font.name = "Segoe UI"
    p2.alignment = PP_ALIGN.CENTER

    # Layout constants
    n = len(PHASES)
    box_w = Inches(1.65)
    box_h = Inches(3.0)
    arrow_w = Inches(0.5)
    arrow_h = Inches(0.3)
    total_w = n * box_w + (n - 1) * arrow_w
    start_x = (prs.slide_width - total_w) / 2
    box_y = Inches(2.4)

    for i, phase in enumerate(PHASES):
        x = start_x + i * (box_w + arrow_w)

        # Darken the phase color for box background
        pc = phase["color"]
        r = max(0, pc[0] // 5)
        g = max(0, pc[1] // 5)
        b = max(0, pc[2] // 5)
        box_bg = RGBColor(r, g, b)

        # Box
        box = add_rounded_box(slide, int(x), int(box_y), int(box_w), int(box_h), box_bg)
        box.line.color.rgb = phase["color"]
        box.line.width = Pt(1.5)

        # Number circle
        circle_size = Inches(0.55)
        cx = int(x + (box_w - circle_size) / 2)
        cy = int(box_y + Inches(0.3))
        circ = slide.shapes.add_shape(MSO_SHAPE.OVAL, cx, cy, int(circle_size), int(circle_size))
        circ.fill.solid()
        circ.fill.fore_color.rgb = phase["color"]
        circ.line.fill.background()
        circ.shadow.inherit = False
        ctf = circ.text_frame
        ctf.word_wrap = False
        cp = ctf.paragraphs[0]
        cp.text = phase["num"]
        cp.font.size = Pt(20)
        cp.font.bold = True
        cp.font.color.rgb = WHITE if i not in (2, 3, 4) else RGBColor(0x0F, 0x17, 0x2A)
        cp.font.name = "Segoe UI"
        cp.alignment = PP_ALIGN.CENTER
        ctf.paragraphs[0].space_before = Pt(0)
        ctf.paragraphs[0].space_after = Pt(0)

        # Title text inside box
        title_y = int(box_y + Inches(1.05))
        ttx = slide.shapes.add_textbox(int(x + Inches(0.1)), title_y, int(box_w - Inches(0.2)), Inches(0.5))
        ttf = ttx.text_frame
        ttf.word_wrap = True
        tp = ttf.paragraphs[0]
        tp.text = phase["title"]
        tp.font.size = Pt(15)
        tp.font.bold = True
        tp.font.color.rgb = WHITE
        tp.font.name = "Segoe UI"
        tp.alignment = PP_ALIGN.CENTER

        # Subtitle text inside box
        sub_y = int(box_y + Inches(1.6))
        stx = slide.shapes.add_textbox(int(x + Inches(0.1)), sub_y, int(box_w - Inches(0.2)), Inches(1.2))
        stf = stx.text_frame
        stf.word_wrap = True
        sp = stf.paragraphs[0]
        sp.text = phase["sub"]
        sp.font.size = Pt(13)
        sp.font.color.rgb = MUTED
        sp.font.name = "Segoe UI"
        sp.alignment = PP_ALIGN.CENTER
        sp.line_spacing = Pt(18)

        # Arrow between boxes (not after last)
        if i < n - 1:
            ax = int(x + box_w + Inches(0.02))
            ay = int(box_y + (box_h - arrow_h) / 2)
            add_arrow(slide, ax, ay, int(arrow_w - Inches(0.04)), int(arrow_h))

    # Speaker notes
    notes_slide = slide.notes_slide
    notes_slide.notes_text_frame.text = (
        "This slide shows the six-stage deterministic pipeline in execution order.\n\n"
        "Stage 1 — Ingestion: GitHub issues are pulled into a versioned in-memory cache "
        "via authenticated MCP sync, the rate-limited public API, or the seed endpoint.\n\n"
        "Stage 2 — Chaos Metrics: Five boolean signals are evaluated first — mystery meat, "
        "unresolved urgents, context switching, after-hours activity, and label sprawl — "
        "producing a chaos score from 0 to 10. This runs BEFORE classification.\n\n"
        "Stage 3 — Classification & Compliance: Each issue is sorted into one of four buckets "
        "(deep work, quick win, maintenance, deferred) and checked against eight compliance rules. "
        "The 3-3-3 day plan is also built here, with classification running a second time internally.\n\n"
        "Stage 4 — Stress Score: The WorldState aggregates chaos metrics, classification counts, "
        "and compliance into 12 capped variables. Six components (workload, chaos, context switching, "
        "clarity, sustained load, after-hours) are summed to produce a score from 0 to 100.\n\n"
        "Stage 5 — AI Agents: Only on reshape. A LangChain4j supervisor receives the fully-calculated "
        "WorldState and orchestrates five sub-agents that recommend label mutations. "
        "Every agent has a deterministic fallback when the LLM is unavailable.\n\n"
        "Stage 6 — Output: Mutations are applied to the in-memory cache (not GitHub directly). "
        "Stress is recalculated on the mutated issues to show the drop. "
        "A Friday deploy score assesses release readiness. "
        "Every check-in is persisted as a JPA snapshot for longitudinal tracking."
    )

    out = "docs/Burnout-Pipeline-Sequence.pptx"
    prs.save(out)
    print(f"Saved: {out}")


if __name__ == "__main__":
    main()
