"use client";

import { useRef } from "react";
import type { StaffPressIdDTO, StaffProfileForSelfDTO } from "@/types/blog";
import { DEPARTMENT_LABELS } from "@/lib/staff";
import { formatDate } from "@/lib/utils";

type PressIdCardProps = {
  data: StaffPressIdDTO | StaffProfileForSelfDTO;
  variant?: "public" | "self";
  showButtons?: boolean;
  className?: string;
  onPrint?: () => void;
  onDownloadPdf?: () => Promise<void>;
  busyPdf?: boolean;
};

function isSelf(d: any): d is StaffProfileForSelfDTO {
  return (d as StaffProfileForSelfDTO).mobilePrivate !== undefined &&
    (d as StaffProfileForSelfDTO).mobilePrivate !== null;
}

function safeField(val: string | null | undefined, fallback = "—"): string {
  return val && val.trim() ? val : fallback;
}

export default function PressIdCard({
  data,
  variant = "public",
  showButtons = true,
  className = "",
  onPrint,
  onDownloadPdf,
  busyPdf = false,
}: PressIdCardProps) {
  const stageRef = useRef<HTMLDivElement>(null);
  const self = variant === "self" && isSelf(data);

  const publicData = data as StaffPressIdDTO;
  const selfData = data as StaffProfileForSelfDTO;
  const mobile = self
    ? safeField(selfData.mobilePrivate ?? selfData.workMobile)
    : safeField(publicData.mobileMasked);
  const email = self
    ? safeField(selfData.workEmail ?? selfData.personalEmail)
    : safeField(publicData.workEmailMasked);
  const bloodGroup = self
    ? safeField(selfData.bloodGroup)
    : safeField(publicData.bloodGroupMasked);
  const emergency = self
    ? `${safeField((data as StaffProfileForSelfDTO).emergencyContactName)} · ${safeField((data as StaffProfileForSelfDTO).emergencyNumber)}`
    : null;
  const photoUrl = data.photoUrl || "/assets/placeholders/avatar.svg";
  const signatureUrl = data.signatureUrl || "";
  const serialSuf = data.idNumber.match(/\d{6}$/)?.[0] ?? "000001";

  function doPrint() {
    if (onPrint) {
      onPrint();
    } else {
      window.print();
    }
  }

  async function doPdf() {
    if (onDownloadPdf) await onDownloadPdf();
  }

  return (
    <>
      <PressIdCardCss />
      <div className={`press-id-wrap ${className}`}>
        {showButtons ? (
          <div className="press-hint">
            Premium credential · CR80 proportions · scaled for print-detail preview
          </div>
        ) : null}

        <div className="press-stage" ref={stageRef}>
          {/* =============== FRONT =============== */}
          <div className="press-card press-front">
            <div className="press-punch" />
            <div className="press-microtext">
              <span>
                MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;
              </span>
            </div>

            <div className="press-front-top">
              <img src="/assets/logos/mapmytimes-logo.png" alt="MapMyTimes" />
              <div className="press-brand-text">
                <div className="press-wordmark">
                  MapMy<span>Times</span>
                </div>
                <div className="press-tagline">Independent Digital Newsroom</div>
              </div>
            </div>

            <div className="press-banner">
              <div className="press-banner-t">PRESS</div>
              <div className="press-banner-sub">OFFICIAL NEWS GATHERING CREDENTIAL</div>
            </div>

            <div className="press-photo-wrap">
              {data.photoUrl ? (
                <div className="press-photo-box">
                  <img src={photoUrl} alt={data.fullName} />
                </div>
              ) : (
                <div className="press-photo-box-ph">No photo</div>
              )}
            </div>

            <div className="press-id-block">
              <div className="press-name">{data.fullName}</div>
              <div className="press-role">
                {(data.designation || DEPARTMENT_LABELS[data.department] || "Journalist").toUpperCase()}
              </div>
            </div>

            <div className="press-fields">
              <div className="press-fields-row">
                <div className="press-fields-k">ID Number</div>
                <div className="press-fields-v press-fields-accent">{data.idNumber}</div>
              </div>
              <div className="press-fields-row">
                <div className="press-fields-k">Department</div>
                <div className="press-fields-v">{DEPARTMENT_LABELS[data.department] || String(data.department)}</div>
              </div>
              <div className="press-fields-row">
                <div className="press-fields-k">Location</div>
                <div className="press-fields-v">
                  {[data.city, data.district, data.state].filter(Boolean).join(", ") || "—"}
                </div>
              </div>
              <div className="press-fields-row">
                <div className="press-fields-k">Mobile</div>
                <div className="press-fields-v">{mobile}</div>
              </div>
              <div className="press-fields-row">
                <div className="press-fields-k">Email</div>
                <div className="press-fields-v">{email}</div>
              </div>
              <div className="press-fields-row">
                <div className="press-fields-k">Blood Group</div>
                <div className="press-fields-v">{bloodGroup}</div>
              </div>
              <div className="press-fields-row">
                <div className="press-fields-k">Valid Till</div>
                <div className="press-fields-v press-fields-accent">
                  {formatDate(data.validTill ?? undefined)}
                </div>
              </div>
            </div>

            <div className="press-front-bottom">
              <div className="press-sig">
                <div className="press-sig-line" />
                {signatureUrl ? (
                  <img src={signatureUrl} alt="signature" style={{ maxWidth: 120, maxHeight: 24, objectFit: "contain" }} />
                ) : (
                  "Authorized Signatory"
                )}
              </div>
              <div className="press-security-corner">
                <div className="press-hologram" />
                <div className="press-nfc">
                  <svg viewBox="0 0 24 24" fill="none" stroke="#8A8A8A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M6.3 13.7a9 9 0 0111.4 0" />
                    <path d="M9.1 10.9a5 5 0 015.8 0" />
                    <path d="M12 8a8 8 0 010 13.86" />
                    <circle cx="12" cy="17" r="1.2" fill="#8A8A8A" />
                  </svg>
                  <span>NFC · EID</span>
                </div>
              </div>
            </div>
          </div>

          {/* =============== BACK =============== */}
          <div className="press-card press-back">
            <div className="press-punch" />
            <div className="press-microtext">
              <span>
                MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;
              </span>
            </div>

            <div className="press-back-watermark">
              <img src="/assets/logos/mapmytimes-logo.png" alt="" />
            </div>

            <div className="press-back-content">
              <div className="press-back-head">
                <div className="press-back-head-t">TERMS &amp; CONDITIONS</div>
                <div className="press-back-head-rule" />
              </div>

              <ul className="press-terms">
                <li>This card remains the property of MapMyTimes / MAPMYTOUR LLP.</li>
                <li>Valid only with a government-issued photo ID.</li>
                <li>Identifies the holder for news-gathering purposes only — confers no legal privilege.</li>
                <li>Non-transferable. Report loss or theft immediately.</li>
                <li>Misuse leads to cancellation and possible legal action.</li>
              </ul>

              <div className="press-divider" />

              <div className="press-info-grid">
                <div className="press-info-cell">
                  <div className="press-info-k">Issue Date</div>
                  <div className="press-info-v">{formatDate(data.issueDate ?? undefined)}</div>
                </div>
                <div className="press-info-cell">
                  <div className="press-info-k">Expiry Date</div>
                  <div className="press-info-v">{formatDate(data.validTill ?? undefined)}</div>
                </div>
                <div className="press-info-cell press-info-full">
                  <div className="press-info-k">Emergency Contact</div>
                  <div className="press-info-v">
                    {emergency ?? "Contact newsroom for emergencies"}
                  </div>
                </div>
              </div>

              <div className="press-verify-box">
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=300x300&margin=0&data=${encodeURIComponent(
                    typeof window !== "undefined"
                      ? `${window.location.origin}/verify-press?id=${encodeURIComponent(data.idNumber)}`
                      : `/verify-press?id=${encodeURIComponent(data.idNumber)}`,
                  )}`}
                  alt="QR verification code"
                />
                <div className="press-verify-scan-lbl">SCAN TO VERIFY</div>
                <div className="press-verify-url">verify.mapmytimes.com</div>
                <div className="press-verify-id">VID · {data.idNumber}</div>
              </div>

              <div className="press-back-sig-row">
                <div className="press-back-sig">
                  <div className="press-sig-line" />
                  Authorized Signatory
                </div>
                <div className="press-seal">
                  <svg viewBox="0 0 24 24">
                    <path d="M12 14a3 3 0 003-3V5a3 3 0 00-6 0v6a3 3 0 003 3zm5-3a5 5 0 01-10 0H5a7 7 0 006 6.92V21h2v-3.08A7 7 0 0019 11h-2z" />
                  </svg>
                </div>
              </div>

              <div className="press-org-block">
                <div className="press-org-name">MapMyTimes</div>
                <div className="press-org-sub">
                  Independent Digital Newsroom · A Brand of MAPMYTOUR LLP
                </div>
                <div className="press-org-addr">
                  DDA Flats Kalkaji, Block L1, New Delhi, Delhi 110019
                  <br />
                  Office: +91 98939 89395
                  <br />
                  mapmytimes.com&nbsp;&nbsp;·&nbsp;&nbsp;support@mapmytimes.com
                </div>
              </div>
            </div>

            <div className="press-serial">S/N · MMT-{new Date().getFullYear()}-{serialSuf}</div>
            <div className="press-microtext press-microtext-bottom">
              <span>
                MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;MAPMYTIMES&nbsp;&nbsp;•&nbsp;&nbsp;
              </span>
            </div>
          </div>
        </div>

        {showButtons ? (
          <div className="press-btn-row">
            <button type="button" className="press-print-btn" onClick={doPrint}>
              🖨️ Print / Save as PDF
            </button>
            {onDownloadPdf ? (
              <button
                type="button"
                className="press-pdf-btn"
                onClick={() => void doPdf()}
                disabled={busyPdf}
              >
                {busyPdf ? "Generating…" : "📥 Download PDF"}
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
    </>
  );
}

export { PressIdCardCss };

function PressIdCardCss() {
  return (
    <style
      dangerouslySetInnerHTML={{
        __html: `
:root{
  --press-red:#C8102E;
  --press-black:#0A0A0A;
  --press-white:#FFFFFF;
  --press-bg:#FAF8F4;
  --press-gold:#C9A227;
  --press-gold-light:#F1DFA0;
  --press-grey:#6B6B6B;
  --press-grey-light:#8A8A8A;
  --press-photo-bg:#ECE9E2;
  --press-line:rgba(10,10,10,0.12);
  --press-line-soft:rgba(10,10,10,0.07);
}
.press-id-wrap{
  display:flex;
  flex-direction:column;
  align-items:center;
  gap:40px;
  padding:24px 8px;
  font-family:'Inter',sans-serif;
}
.press-hint{
  color:#8b919a;
  font-size:11px;
  letter-spacing:.12em;
  text-transform:uppercase;
}
.press-stage{
  display:flex;
  gap:44px;
  flex-wrap:wrap;
  justify-content:center;
  align-items:flex-start;
}
.press-card{
  width:360px;
  height:571px;
  border-radius:16px;
  position:relative;
  overflow:hidden;
  box-shadow:0 24px 60px rgba(0,0,0,0.18), 0 4px 10px rgba(0,0,0,0.12);
  color:var(--press-black);
  background:var(--press-bg);
  display:flex;
  flex-direction:column;
}
.press-card::before{
  content:"";
  position:absolute; inset:0;
  background-image:
    repeating-radial-gradient(circle at 18% 12%, rgba(200,16,46,0.030) 0px, rgba(200,16,46,0.030) 1px, transparent 1px, transparent 9px),
    repeating-radial-gradient(circle at 88% 92%, rgba(10,10,10,0.028) 0px, rgba(10,10,10,0.028) 1px, transparent 1px, transparent 9px),
    repeating-linear-gradient(115deg, rgba(10,10,10,0.02) 0px, rgba(10,10,10,0.02) 1px, transparent 1px, transparent 8px);
  pointer-events:none;
  z-index:0;
}
.press-card > *{ position:relative; z-index:1; }

.press-microtext{
  position:relative;
  height:12px;
  overflow:hidden;
  white-space:nowrap;
  display:flex;
  align-items:center;
  background:var(--press-bg);
  border-bottom:1px solid var(--press-line-soft);
  flex-shrink:0;
}
.press-microtext-bottom{ border-bottom:none; border-top:1px solid var(--press-line-soft); }
.press-microtext span{
  font-family:'Inter',sans-serif;
  font-size:4.6px;
  font-weight:600;
  letter-spacing:2.2px;
  color:rgba(10,10,10,0.30);
  text-transform:uppercase;
}

.press-punch{
  position:absolute;
  top:16px; left:50%;
  transform:translateX(-50%);
  width:28px; height:8px;
  border-radius:5px;
  background:#1c1e22;
  z-index:5;
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.6);
}

.press-front-top{
  padding:26px 20px 0;
  display:flex;
  flex-direction:row;
  align-items:center;
  justify-content:center;
  gap:10px;
}
.press-front-top img{ height:30px; width:auto; flex-shrink:0; }
.press-brand-text{ text-align:left; line-height:1; }
.press-wordmark{
  font-family:'Poppins',sans-serif;
  font-size:14px;
  font-weight:700;
  letter-spacing:.3px;
  color:var(--press-black);
  white-space:nowrap;
}
.press-wordmark span{ color:var(--press-red); }
.press-tagline{
  margin-top:3px;
  font-family:'Inter',sans-serif;
  font-size:6px;
  font-weight:600;
  letter-spacing:2.2px;
  color:var(--press-red);
  text-transform:uppercase;
  white-space:nowrap;
}

.press-banner{
  margin:14px 20px 0;
  padding:8px 0;
  text-align:center;
  border-top:1px solid var(--press-black);
  border-bottom:1px solid var(--press-black);
}
.press-banner-t{
  font-family:'Bebas Neue',sans-serif;
  font-size:27px;
  letter-spacing:10px;
  color:var(--press-black);
  padding-left:10px;
}
.press-banner-sub{
  margin-top:1px;
  font-family:'Inter',sans-serif;
  font-size:5.6px;
  font-weight:600;
  letter-spacing:2.4px;
  color:var(--press-grey);
  text-transform:uppercase;
}

.press-photo-wrap{ display:flex; justify-content:center; margin-top:18px; }
.press-photo-box, .press-photo-box-ph{
  width:150px; height:178px;
  background:var(--press-photo-bg);
  border:2px solid var(--press-black);
  border-radius:4px;
  overflow:hidden;
}
.press-photo-box img{ width:100%; height:100%; object-fit:cover; display:block; }
.press-photo-box-ph{
  display:flex; align-items:center; justify-content:center;
  color:#9c988c; font-family:'Inter',sans-serif; font-size:9px;
  letter-spacing:1.5px; text-transform:uppercase;
}

.press-id-block{ text-align:center; margin-top:14px; padding:0 20px; }
.press-name{
  font-family:'Poppins',sans-serif;
  font-size:20px;
  font-weight:700;
  letter-spacing:.2px;
  line-height:1.08;
}
.press-role{
  display:inline-block;
  margin-top:6px;
  font-family:'Inter',sans-serif;
  font-size:9px;
  font-weight:700;
  letter-spacing:2px;
  color:#fff;
  background:var(--press-red);
  padding:3.5px 12px;
  border-radius:2px;
  text-transform:uppercase;
}

.press-fields{ margin-top:16px; padding:0 22px; }
.press-fields-row{
  display:flex; justify-content:space-between; align-items:baseline;
  padding:5px 0; border-bottom:1px solid var(--press-line);
}
.press-fields-row:last-child{ border-bottom:none; }
.press-fields-k{
  font-family:'Inter',sans-serif; font-size:7.4px; font-weight:600;
  letter-spacing:1.2px; color:var(--press-grey); text-transform:uppercase;
}
.press-fields-v{
  font-family:'Inter',sans-serif; font-size:11px; font-weight:700;
  color:var(--press-black); letter-spacing:.2px; text-align:right; max-width:62%;
}
.press-fields-accent{ color:var(--press-red); }

.press-front-bottom{
  margin-top:auto;
  padding:12px 20px 16px;
  display:flex; align-items:flex-end; justify-content:space-between;
}
.press-sig, .press-back-sig{
  font-family:'Inter',sans-serif;
  font-size:6.6px;
  color:var(--press-grey);
  text-transform:uppercase;
  letter-spacing:.6px;
  display:flex; flex-direction:column;
}
.press-sig-line{
  width:120px;
  border-top:1px solid var(--press-black);
  margin-bottom:4px;
}
.press-back-sig .press-sig-line{ width:96px; }
.press-security-corner{ display:flex; align-items:center; gap:8px; }
.press-hologram{
  width:36px; height:36px;
  border-radius:50%;
  position:relative;
  background: conic-gradient(from 200deg,
    #f5e6ae, var(--press-gold), #fffbe8, #a5791b, #f5e6ae, var(--press-gold), #fffbe8);
  box-shadow: 0 0 0 1px rgba(10,10,10,0.15), inset 0 0 6px rgba(255,255,255,0.5);
}
.press-hologram::after{
  content:"MMT";
  position:absolute; inset:0;
  display:flex; align-items:center; justify-content:center;
  font-family:'Inter',sans-serif;
  font-size:5px; font-weight:700;
  letter-spacing:1px;
  color:rgba(80,50,10,0.55);
}
.press-nfc{
  display:flex; flex-direction:column; align-items:center; gap:2px;
}
.press-nfc svg{ width:15px; height:15px; }
.press-nfc span{
  font-family:'Inter',sans-serif; font-size:4.6px; font-weight:700;
  letter-spacing:1px; color:var(--press-grey-light);
}

.press-back-content{
  padding:18px 20px 12px; display:flex; flex-direction:column; flex:1; min-height:0;
}
.press-back-watermark{
  position:absolute; right:-30px; bottom:40px; width:150px;
  opacity:0.04; pointer-events:none; z-index:0;
}
.press-back-head{ text-align:center; margin-bottom:10px; }
.press-back-head-t{
  font-family:'Bebas Neue',sans-serif; font-size:15px;
  letter-spacing:3px; color:var(--press-black);
}
.press-back-head-rule{
  width:36px; height:2px;
  background:var(--press-red);
  margin:5px auto 0;
}
.press-terms{
  font-family:'Inter',sans-serif; font-size:7.6px;
  line-height:1.5; color:#2b2b2b;
  list-style:none; padding:0; margin:0;
}
.press-terms li{ position:relative; padding-left:10px; margin-bottom:4px; }
.press-terms li::before{
  content:"—";
  position:absolute; left:0; top:0; color:var(--press-red);
}
.press-divider{ height:1px; background:var(--press-line); margin:11px 0; }
.press-info-grid{
  display:grid; grid-template-columns:1fr 1fr;
  row-gap:8px; column-gap:10px;
}
.press-info-full{ grid-column:span 2; }
.press-info-k{
  font-family:'Inter',sans-serif; font-size:6.4px; font-weight:600;
  letter-spacing:1px; color:var(--press-grey); text-transform:uppercase;
}
.press-info-v{
  font-family:'Inter',sans-serif; font-size:9.6px; font-weight:700;
  color:var(--press-black); margin-top:1px;
}
.press-verify-box{
  margin-top:12px;
  border:1px solid var(--press-black);
  border-radius:6px;
  padding:12px;
  display:flex; flex-direction:column; align-items:center;
  position:relative;
}
.press-verify-box::before, .press-verify-box::after{
  content:""; position:absolute; width:8px; height:8px;
  border-color:var(--press-gold); border-style:solid;
}
.press-verify-box::before{ top:-1px; left:-1px; border-width:2px 0 0 2px; }
.press-verify-box::after{ bottom:-1px; right:-1px; border-width:0 2px 2px 0; }
.press-verify-box img{ width:82px; height:82px; }
.press-verify-scan-lbl{
  margin-top:8px;
  font-family:'Bebas Neue',sans-serif;
  font-size:11px; letter-spacing:2.4px;
  color:var(--press-black);
}
.press-verify-url{
  margin-top:1px;
  font-family:'Inter',sans-serif; font-size:7.4px;
  font-weight:600; color:var(--press-red);
}
.press-verify-id{
  margin-top:3px;
  font-family:'Inter',sans-serif; font-size:6px;
  letter-spacing:.6px; color:var(--press-grey-light);
}
.press-back-sig-row{
  margin-top:12px;
  display:flex; align-items:center; justify-content:space-between;
}
.press-seal{
  width:42px; height:42px;
  border-radius:50%;
  border:1.3px solid var(--press-black);
  display:flex; align-items:center; justify-content:center;
  position:relative; flex-shrink:0;
}
.press-seal::before{
  content:""; position:absolute; inset:3px;
  border-radius:50%;
  border:1px dashed var(--press-grey-light);
}
.press-seal svg{ width:16px; height:16px; fill:var(--press-red); }
.press-org-block{
  margin-top:auto;
  padding-top:10px;
  border-top:1px solid var(--press-line);
  text-align:center;
}
.press-org-name{
  font-family:'Poppins',sans-serif;
  font-size:9.5px; font-weight:700;
}
.press-org-sub{
  font-family:'Inter',sans-serif;
  font-size:6.4px; color:var(--press-grey); margin-top:1px;
}
.press-org-addr{
  font-family:'Inter',sans-serif;
  font-size:6.2px; color:var(--press-grey);
  line-height:1.4; margin-top:5px;
}
.press-serial{
  text-align:center;
  padding:6px 20px 0;
  font-family:'Inter',sans-serif; font-size:6px; font-weight:600;
  letter-spacing:1.4px; color:var(--press-grey-light);
  flex-shrink:0;
}

.press-btn-row{
  display:flex; flex-wrap:wrap; gap:12px; justify-content:center;
}
.press-print-btn, .press-pdf-btn{
  font-family:'Inter',sans-serif; font-size:13px; color:#fff;
  background:var(--press-red);
  border:none; padding:11px 22px; border-radius:6px;
  cursor:pointer; font-weight:600;
  box-shadow:0 6px 18px rgba(200,16,46,0.25);
  transition:transform .12s ease, box-shadow .12s ease;
}
.press-pdf-btn{ background:var(--press-black); box-shadow:0 6px 18px rgba(10,10,10,0.25); }
.press-print-btn:hover, .press-pdf-btn:hover{
  transform:translateY(-1px);
}
.press-pdf-btn:disabled{ opacity:.6; cursor:not-allowed; }

@media print{
  body{ background:#fff !important; padding:0 !important; }
  .press-hint, .press-btn-row, header, footer, .site-header, .site-footer, nav, aside{ display:none !important; }
  main{ padding:0 !important; }
  .press-id-wrap{ padding:0 !important; gap:0 !important; }
  .press-stage{ gap:0 !important; }
  .press-card{
    box-shadow:none !important;
    page-break-after:always;
    transform:scale(1);
    margin:0 auto;
  }
}
`,
      }}
    />
  );
}
