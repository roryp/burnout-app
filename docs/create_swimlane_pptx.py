"""Clean swimlane PPTX — labels only, all detail in speaker notes."""
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

BG = RGBColor(0x0F, 0x17, 0x2A)
WHITE = RGBColor(0xF8, 0xFA, 0xFC)
MUTED = RGBColor(0x94, 0xA3, 0xB8)
DIM = RGBColor(0x47, 0x55, 0x69)
LANE_BG = RGBColor(0x14, 0x1E, 0x30)
LANE_BORDER = RGBColor(0x33, 0x44, 0x55)

C_INGEST = RGBColor(0x3B, 0x82, 0xF6)
C_CHAOS  = RGBColor(0xF5, 0x9E, 0x0B)
C_CLASS  = RGBColor(0xA7, 0x8B, 0xFA)
C_STRESS = RGBColor(0x2D, 0xD4, 0xBF)
C_AGENT  = RGBColor(0xFB, 0x71, 0x85)
C_OUTPUT = RGBColor(0x81, 0x8C, 0xF8)
C_LOOP   = RGBColor(0xF5, 0x9E, 0x0B)


def set_bg(slide):
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = BG


def lane_bg(slide, l, t, w, h):
    s = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, l, t, w, h)
    s.fill.solid()
    s.fill.fore_color.rgb = LANE_BG
    s.line.color.rgb = LANE_BORDER
    s.line.width = Pt(0.75)
    s.shadow.inherit = False


def rbox(slide, l, t, w, h, c):
    s = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, l, t, w, h)
    s.fill.solid()
    s.fill.fore_color.rgb = RGBColor(c[0] // 5, c[1] // 5, c[2] // 5)
    s.line.color.rgb = c
    s.line.width = Pt(1.5)
    s.shadow.inherit = False
    s.adjustments[0] = 0.15


def txt(slide, l, t, w, h, text, size=14, color=WHITE, bold=False):
    tb = slide.shapes.add_textbox(l, t, w, h)
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = text
    p.font.size = Pt(size)
    p.font.color.rgb = color
    p.font.bold = bold
    p.font.name = "Segoe UI"
    p.alignment = PP_ALIGN.CENTER
    p.space_before = Pt(0)
    p.space_after = Pt(0)


def harrow(slide, x, y, w, color=DIM):
    s = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, x, y, w, Inches(0.2))
    s.fill.solid()
    s.fill.fore_color.rgb = color
    s.line.fill.background()
    s.shadow.inherit = False


def darrow(slide, x, y, h, color=DIM):
    s = slide.shapes.add_shape(MSO_SHAPE.DOWN_ARROW, x, y, Inches(0.2), h)
    s.fill.solid()
    s.fill.fore_color.rgb = color
    s.line.fill.background()
    s.shadow.inherit = False


def main():
    prs = Presentation()
    prs.slide_width = SW = Inches(13.333)
    prs.slide_height = Inches(7.5)

    slide = prs.slides.add_slide(prs.slide_layouts[6])
    set_bg(slide)

    txt(slide, 0, Inches(0.3), SW, Inches(0.5), "Reshape Pipeline", 34, WHITE, True)

    # Lane geometry
    lx = Inches(2.3); lw = Inches(10.5)
    y1 = Inches(1.2); h1 = Inches(1.55)
    y2 = Inches(3.0); h2 = Inches(1.35)
    y3 = Inches(4.6); h3 = Inches(1.35)

    for y, h in [(y1, h1), (y2, h2), (y3, h3)]:
        lane_bg(slide, lx, y, lw, h)

    # Lane labels
    lbl_x = Inches(0.2); lbl_w = Inches(1.9)
    txt(slide, lbl_x, y1 + Inches(0.5), lbl_w, Inches(0.3), "DETERMINISTIC", 14, C_STRESS, True)
    txt(slide, lbl_x, y2 + Inches(0.35), lbl_w, Inches(0.3), "AI AGENTS", 14, C_AGENT, True)
    txt(slide, lbl_x, y2 + Inches(0.65), lbl_w, Inches(0.25), "(reshape only)", 11, MUTED)
    txt(slide, lbl_x, y3 + Inches(0.45), lbl_w, Inches(0.3), "OUTPUT", 14, C_OUTPUT, True)

    # ── Lane 1 boxes ──
    bw = Inches(2.15); bh = Inches(0.85)
    by = y1 + Inches(0.35); gap = Inches(0.45)
    x1 = lx + Inches(0.3)
    x2 = x1 + bw + gap
    x3 = x2 + bw + gap
    x4 = x3 + bw + gap

    for x, c, label in [
        (x1, C_INGEST, "Ingestion"),
        (x2, C_CHAOS,  "Chaos Metrics"),
        (x3, C_CLASS,  "Classify + Comply"),
        (x4, C_STRESS, "Stress Score"),
    ]:
        rbox(slide, x, by, bw, bh, c)
        txt(slide, x, by + Inches(0.22), bw, Inches(0.4), label, 17, WHITE, True)

    for x in [x1, x2, x3]:
        harrow(slide, x + bw + Inches(0.05), by + bh / 2 - Inches(0.1), gap - Inches(0.1))

    # Arrow down
    darrow(slide, x4 + bw / 2 - Inches(0.1), by + bh + Inches(0.05), y2 - by - bh + Inches(0.25))

    # ── Lane 2 box ──
    ai_bw = Inches(4.5); ai_bh = Inches(0.85)
    ai_x = lx + (lw - ai_bw) / 2
    ai_y = y2 + Inches(0.25)

    rbox(slide, ai_x, ai_y, ai_bw, ai_bh, C_AGENT)
    txt(slide, ai_x, ai_y + Inches(0.22), ai_bw, Inches(0.4),
        "Supervisor → Sub-Agents → Label Mutations", 17, WHITE, True)

    # Arrow down
    darrow(slide, ai_x + ai_bw / 2 - Inches(0.1), ai_y + ai_bh + Inches(0.05),
           y3 - ai_y - ai_bh + Inches(0.2))

    # ── Lane 3 boxes ──
    obw = Inches(2.2); obh = Inches(0.85)
    oy = y3 + Inches(0.25); ogap = Inches(0.38)
    ox1 = lx + Inches(0.3)
    ox2 = ox1 + obw + ogap
    ox3 = ox2 + obw + ogap
    ox4 = ox3 + obw + ogap

    for ox, c, label in [
        (ox1, C_OUTPUT, "Apply Mutations"),
        (ox2, C_LOOP,   "Recalculate ↻"),
        (ox3, C_STRESS, "Friday Score"),
        (ox4, C_OUTPUT, "Persist Snapshot"),
    ]:
        rbox(slide, ox, oy, obw, obh, c)
        txt(slide, ox, oy + Inches(0.22), obw, Inches(0.4), label, 16, WHITE, True)

    for ox in [ox1, ox2, ox3]:
        harrow(slide, ox + obw + Inches(0.02), oy + obh / 2 - Inches(0.1), ogap - Inches(0.04))

    # Tagline
    txt(slide, 0, Inches(6.35), SW, Inches(0.3),
        "Deterministic first  →  AI acts  →  Recalculate  →  Persist", 13, DIM)

    # ── Speaker notes ──
    slide.notes_slide.notes_text_frame.text = (
        "DETERMINISTIC LANE\n"
        "1. Ingestion — Issues loaded from in-memory cache (populated by seed, sync, or MCP).\n"
        "2. Chaos Metrics — Runs FIRST. Five boolean signals each worth 2 points: "
        "mystery meat ≥ 3, urgent ≥ 3, context switching ≥ 6, after-hours, label sprawl ≥ 12. Score 0–10.\n"
        "3. Classify + Comply — Runs SECOND. IssueClassifierService sorts issues into "
        "DEEP_WORK / QUICK_WIN / MAINTENANCE / DEFERRED (first match wins). "
        "ComplianceService checks 8 violation rules (score 100 → 0). "
        "3-3-3 day plan built here too (classification runs twice).\n"
        "4. Stress Score — WorldState aggregates chaos + classification into 12 capped variables. "
        "Six components summed: Workload (0–40), Chaos (0–30), Context Switching (0–15), "
        "Clarity (0–10), Sustained (0–15), After Hours (0–10). Total capped at 100. "
        "This is the BEFORE score.\n\n"
        "AI AGENTS LANE (reshape only — checkin and flamegraph skip this)\n"
        "5. Supervisor receives the fully-calculated WorldState and orchestrates 5 sub-agents. "
        "ClassifyAgent RECLASSIFIES issues by adding labels (quick-win, deep-work, maintenance). "
        "DeferAgent adds deferred + next-sprint. DelegateAgent adds delegated + needs-owner. "
        "ScopeAgent adds needs-scope + blocked. WellnessAgent suggests breaks. "
        "Every agent has a deterministic fallback when LLM is unavailable.\n\n"
        "OUTPUT LANE\n"
        "6a. Apply Mutations — Agent's label changes written to IssueCache (not GitHub directly).\n"
        "6b. Recalculate — The ENTIRE deterministic pipeline reruns on mutated issues. "
        "Chaos recalculated, classification re-runs with new labels → different buckets, "
        "new stress score computed. This is the AFTER score (how 100→10 happens).\n"
        "6c. Friday Score — Deploy readiness: READY ≥ 80, CAUTION ≥ 50, NOT_READY < 50.\n"
        "6d. Persist Snapshot — JPA StressSnapshot with all 6 components + optional self-report. "
        "Powers the Study Dashboard trend charts."
    )

    prs.save("docs/Burnout-Swimlanes.pptx")
    print("Saved: docs/Burnout-Swimlanes.pptx")


if __name__ == "__main__":
    main()
