import { AnimatePresence, motion } from 'framer-motion';

interface ExplanationPanelProps {
  texto: string;
  visible: boolean;
}

export const ExplanationPanel = ({ texto, visible }: ExplanationPanelProps) => {
  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: 'auto', opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          transition={{ duration: 0.3 }}
          className="overflow-hidden rounded-md border bg-muted/50 p-4 text-sm"
        >
          {texto}
        </motion.div>
      )}
    </AnimatePresence>
  );
};
