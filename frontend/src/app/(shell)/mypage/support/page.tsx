"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, AtSign, ChevronDown } from "lucide-react";

interface FaqItem {
  question: string;
  answer: string;
}

const FAQ_ITEMS: FaqItem[] = [
  {
    question: "배송비는 얼마인가요?",
    answer:
      "배송비는 3,400원이며, 7만원 이상 구매 시 무료배송입니다. 제주/도서산간 지역은 배송비가 4,000원 추가됩니다.",
  },
  {
    question: "어떤 결제수단을 이용할 수 있나요?",
    answer: "토스페이먼츠를 통해 신용카드 및 간편결제를 이용하실 수 있습니다.",
  },
  {
    question: "교환/환불은 어떻게 하나요?",
    answer:
      "상품 수령 후 7일 이내 청약철회가 가능합니다. 단순 변심에 의한 교환/환불은 왕복 배송비를 구매자가 부담하며, 상품 하자나 오배송의 경우 판매자가 배송비를 부담합니다. 교환/환불 접수는 인스타그램 DM으로 안내해 드립니다.",
  },
  {
    question: "회원가입/로그인은 어떻게 하나요?",
    answer: "이메일과 비밀번호로 간편하게 회원가입 및 로그인하실 수 있습니다.",
  },
];

export default function SupportPage() {
  const router = useRouter();
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  function toggleFaq(index: number) {
    setOpenIndex((prev) => (prev === index ? null : index));
  }

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center border-b px-4">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">고객센터</span>
        <div className="h-5 w-5" />
      </div>

      <div className="flex flex-1 flex-col gap-6 p-4 pb-28">
        <div className="flex flex-col gap-3">
          <span className="text-title-sm text-ink">자주 묻는 질문</span>
          <div className="border-hairline bg-surface-card flex flex-col rounded-md border">
            {FAQ_ITEMS.map((item, index) => {
              const isOpen = openIndex === index;
              return (
                <div
                  key={item.question}
                  className={index < FAQ_ITEMS.length - 1 ? "border-hairline border-b" : ""}
                >
                  <button
                    onClick={() => toggleFaq(index)}
                    aria-expanded={isOpen}
                    className="flex w-full items-center justify-between gap-3 px-4 py-4 text-left"
                  >
                    <span className="text-body text-ink">{item.question}</span>
                    <ChevronDown
                      className={`text-muted h-4 w-4 flex-shrink-0 transition-transform ${
                        isOpen ? "rotate-180" : ""
                      }`}
                    />
                  </button>
                  {isOpen && (
                    <p className="text-body-sm text-body px-4 pb-4">{item.answer}</p>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <span className="text-title-sm text-ink">문의하기</span>
          <a
            href="https://instagram.com/momentive_official"
            target="_blank"
            rel="noopener noreferrer"
            className="border-hairline bg-surface-card flex items-center gap-3 rounded-md border p-3.5"
          >
            <div className="bg-surface-strong flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full">
              <AtSign className="text-brand-pink-deep h-5 w-5" />
            </div>
            <div className="flex flex-1 flex-col">
              <span className="text-body text-ink">인스타그램으로 문의하기</span>
              <span className="text-body-sm text-muted">@momentive_official</span>
            </div>
          </a>
        </div>
      </div>
    </div>
  );
}
